package com.knowledgehub.knowledge.sync.application;

import com.knowledgehub.knowledge.indexing.application.CommitIndexingService;
import com.knowledgehub.knowledge.indexing.application.IndexingService;
import com.knowledgehub.knowledge.ingestion.application.SourceDeleted;
import com.knowledgehub.knowledge.ingestion.application.SourceNotFoundException;
import com.knowledgehub.knowledge.ingestion.domain.Source;
import com.knowledgehub.knowledge.ingestion.domain.port.SourceRepository;
import com.knowledgehub.knowledge.sync.domain.ChangeSet;
import com.knowledgehub.knowledge.sync.domain.FreshnessInfo;
import com.knowledgehub.knowledge.sync.domain.SyncResult;
import com.knowledgehub.knowledge.sync.domain.port.Evictor;
import com.knowledgehub.knowledge.sync.domain.port.FreshnessRepository;
import com.knowledgehub.knowledge.sync.domain.port.SourceDiffer;
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
 * unchanged chunks are not re-embedded), reconciles the chunks a modified file dropped, indexes the
 * commits that arrived since the last run, records freshness, and announces the change so
 * dependents (e.g. the retrieval cache) can react. Commit indexing runs even when no file changed —
 * a commit does not have to touch an indexed file — and a failure in it never fails the file sync,
 * because the next run picks the same commits up again.
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
  private final CommitIndexingService commitIndexingService;
  private final Evictor evictor;
  private final FreshnessRepository freshness;
  private final ApplicationEventPublisher events;

  public SyncService(
      SourceRepository sources,
      List<SourceDiffer> differs,
      IndexingService indexingService,
      CommitIndexingService commitIndexingService,
      Evictor evictor,
      FreshnessRepository freshness,
      ApplicationEventPublisher events) {
    this.sources = sources;
    this.differs = differs;
    this.indexingService = indexingService;
    this.commitIndexingService = commitIndexingService;
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
      int commitsIndexed = indexCommits(source);
      if (commitsIndexed > 0) {
        // The files are unchanged but the knowledge did grow, so freshness and dependents (e.g.
        // the retrieval cache) must still learn about it.
        freshness.save(
            new FreshnessInfo(
                sourceId, Instant.now(), changes.toCommit(), source.ref().orElse(null)));
        events.publishEvent(new IndexCompleted(sourceId));
      }
      long elapsed = System.currentTimeMillis() - start;
      log.info(
          "Sync of {} found no file changes, {} new commits ({} ms)",
          sourceId,
          commitsIndexed,
          elapsed);
      return SyncResult.noChange(
          sourceId, changes.unchanged(), commitsIndexed, elapsed, changes.toCommit());
    }

    evictor.evictFiles(sourceId, changes.deleted());

    Map<String, List<String>> chunkIdsByPath =
        indexingService.reindex(sourceId, new HashSet<>(changes.toIndex()));
    for (String path : changes.modified()) {
      // Reconcile only when the file was actually re-indexed. A modified file whose re-index was
      // skipped or failed is absent from the map; retaining against an empty keep-set would then
      // evict every chunk of a file that still exists, so leave its old chunks in place to retry.
      if (chunkIdsByPath.containsKey(path)) {
        evictor.retainChunks(sourceId, path, chunkIdsByPath.get(path));
      }
    }

    // After the files, so the MODIFIES edges of fresh commits find their :File targets in place.
    int commitsIndexed = indexCommits(source);

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
            changes.unchanged(),
            commitsIndexed,
            elapsed,
            changes.toCommit(),
            false);
    log.info(
        "Synced {}: +{} ~{} -{} files, {} new commits in {} ms",
        sourceId,
        result.indexed(),
        result.reindexed(),
        result.evicted(),
        result.commitsIndexed(),
        elapsed);
    return result;
  }

  /**
   * Indexes the source's new commits, starting after the commit recorded at the last sync. Never
   * fails the surrounding file sync: commits are append-only and deduplicated, so whatever a failed
   * run missed, the next run indexes.
   */
  private int indexCommits(Source source) {
    String sinceSha = freshness.find(source.sourceId()).map(FreshnessInfo::commitSha).orElse(null);
    try {
      return commitIndexingService.index(source, sinceSha);
    } catch (RuntimeException e) {
      log.warn("Commit indexing failed for source {}: {}", source.sourceId(), e.toString());
      return 0;
    }
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
