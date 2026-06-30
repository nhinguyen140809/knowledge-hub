package com.knowledgehub.knowledge.sync.application;

import com.knowledgehub.knowledge.indexing.application.IndexingService;
import com.knowledgehub.knowledge.ingestion.application.SourceDeleted;
import com.knowledgehub.knowledge.ingestion.application.SourceNotFoundException;
import com.knowledgehub.knowledge.ingestion.domain.Source;
import com.knowledgehub.knowledge.ingestion.domain.SourceRepository;
import com.knowledgehub.knowledge.sync.domain.ChangeSet;
import com.knowledgehub.knowledge.sync.domain.Evictor;
import com.knowledgehub.knowledge.sync.domain.FreshnessInfo;
import com.knowledgehub.knowledge.sync.domain.FreshnessRepository;
import com.knowledgehub.knowledge.sync.domain.SourceDiffer;
import com.knowledgehub.knowledge.sync.domain.SyncResult;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Keeps a source's index consistent with the source by processing only what changed. It diffs the
 * source, evicts deleted files, re-indexes new and modified ones (reusing the indexing pipeline, so
 * unchanged chunks are not re-embedded), reconciles the chunks a modified file dropped, records
 * freshness, and announces the change so dependents (e.g. the retrieval cache) can react.
 *
 * <p>Idempotent: a re-trigger with no change is a no-op. Eviction also runs on a {@link
 * SourceDeleted} event, so removing a source purges its knowledge from both stores.
 */
@Service
public class SyncService {

  private static final Logger log = LoggerFactory.getLogger(SyncService.class);

  private final SourceRepository sources;
  private final List<SourceDiffer> differs;
  private final IndexingService indexingService;
  private final Evictor evictor;
  private final FreshnessRepository freshness;
  private final ApplicationEventPublisher events;

  public SyncService(
      SourceRepository sources,
      List<SourceDiffer> differs,
      IndexingService indexingService,
      Evictor evictor,
      FreshnessRepository freshness,
      ApplicationEventPublisher events) {
    this.sources = sources;
    this.differs = differs;
    this.indexingService = indexingService;
    this.evictor = evictor;
    this.freshness = freshness;
    this.events = events;
  }

  /**
   * Syncs a source: processes its added/modified/deleted files and reports what happened.
   *
   * @param sourceId the source to sync
   * @return the counts of files indexed/re-indexed/evicted/skipped and the new freshness
   * @throws SourceNotFoundException if the source does not exist
   */
  public SyncResult sync(String sourceId) {
    long start = System.currentTimeMillis();
    Source source =
        sources.findById(sourceId).orElseThrow(() -> new SourceNotFoundException(sourceId));
    SourceDiffer differ =
        differs.stream()
            .filter(d -> d.supports(source))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No differ for source " + sourceId));

    ChangeSet changes = differ.diff(source);
    if (changes.isEmpty()) {
      long elapsed = System.currentTimeMillis() - start;
      log.info("Sync of {} found no changes ({} ms)", sourceId, elapsed);
      return SyncResult.noChange(sourceId, 0, elapsed, changes.toCommit());
    }

    evictor.evictFiles(sourceId, changes.deleted());

    Map<String, List<String>> chunkIdsByPath =
        indexingService.reindex(sourceId, new HashSet<>(changes.toIndex()));
    for (String path : changes.modified()) {
      evictor.retainChunks(sourceId, path, chunkIdsByPath.getOrDefault(path, List.of()));
    }

    freshness.save(
        new FreshnessInfo(sourceId, Instant.now(), changes.toCommit(), source.ref().orElse(null)));
    events.publishEvent(new IndexCompleted(sourceId));

    long elapsed = System.currentTimeMillis() - start;
    SyncResult result =
        new SyncResult(
            sourceId,
            changes.added().size(),
            changes.modified().size(),
            changes.deleted().size(),
            0,
            elapsed,
            changes.toCommit(),
            false);
    log.info(
        "Synced {}: +{} ~{} -{} files in {} ms",
        sourceId,
        result.indexed(),
        result.reindexed(),
        result.evicted(),
        elapsed);
    return result;
  }

  /**
   * The freshness of a source's index, or empty if it has never been synced.
   *
   * @throws SourceNotFoundException if the source does not exist
   */
  public Optional<FreshnessInfo> freshnessOf(String sourceId) {
    if (sources.findById(sourceId).isEmpty()) {
      throw new SourceNotFoundException(sourceId);
    }
    return freshness.find(sourceId);
  }

  /** Purges a removed source's knowledge from both stores and drops its freshness record. */
  @EventListener
  public void onSourceDeleted(SourceDeleted event) {
    evictor.evictSource(event.sourceId());
    freshness.deleteBySource(event.sourceId());
    events.publishEvent(new IndexCompleted(event.sourceId()));
    log.info("Evicted all knowledge for removed source {}", event.sourceId());
  }
}
