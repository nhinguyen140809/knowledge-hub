package com.knowledgehub.knowledge.indexing.application;

import com.knowledgehub.knowledge.indexing.domain.ChunkConfig;
import com.knowledgehub.knowledge.ingestion.application.IngestionService;
import com.knowledgehub.knowledge.ingestion.domain.RawArtifact;
import com.knowledgehub.shared.config.AppProperties;
import com.knowledgehub.shared.pipeline.Pipeline;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Indexes a source by running each ingested artifact through the chunk → dedup → embed → store
 * pipeline. This service is only wiring: it pulls artifacts from ingestion, builds a per-artifact
 * context, runs the pipeline, and tallies the result. All real work lives in the stages, so sync
 * can reuse them on a delta. A failure on one artifact is isolated and the rest of the run
 * continues; the embedding call is never inside a database transaction.
 */
@Service
public class IndexingService {

  private static final Logger log = LoggerFactory.getLogger(IndexingService.class);

  private final IngestionService ingestion;
  private final AppProperties properties;
  private final Pipeline<IndexingContext> pipeline;

  public IndexingService(
      IngestionService ingestion,
      AppProperties properties,
      ChunkStage chunkStage,
      DedupStage dedupStage,
      EmbedStage embedStage,
      StoreStage storeStage,
      LinkStage linkStage) {
    this.ingestion = ingestion;
    this.properties = properties;
    this.pipeline =
        new Pipeline<>(List.of(chunkStage, dedupStage, embedStage, storeStage, linkStage));
  }

  /**
   * Indexes every artifact of the source.
   *
   * @param sourceId the source to index
   * @return counts of files and chunks processed
   * @throws com.knowledgehub.knowledge.ingestion.application.SourceNotFoundException if unknown
   */
  public IndexResult index(String sourceId) {
    ChunkConfig config =
        new ChunkConfig(properties.chunk().maxTokens(), properties.chunk().overlap());
    AtomicInteger filesRead = new AtomicInteger();
    AtomicInteger filesSkipped = new AtomicInteger();
    AtomicInteger chunksIndexed = new AtomicInteger();
    AtomicInteger chunksCached = new AtomicInteger();
    AtomicInteger relationshipsLinked = new AtomicInteger();

    try (Stream<RawArtifact> artifacts = ingestion.ingest(sourceId)) {
      artifacts.forEach(
          artifact -> {
            try {
              IndexingContext context = pipeline.run(new IndexingContext(artifact, config));
              if (context.isSkipped()) {
                filesSkipped.incrementAndGet();
              } else {
                filesRead.incrementAndGet();
                chunksIndexed.addAndGet(context.newChunks().size());
                chunksCached.addAndGet(context.cached());
                relationshipsLinked.addAndGet(context.relationshipsLinked());
              }
            } catch (RuntimeException e) {
              filesSkipped.incrementAndGet();
              log.warn(
                  "Skipping artifact {} in source {}: {}", artifact.path(), sourceId, e.toString());
            }
          });
    }

    IndexResult result =
        new IndexResult(
            sourceId,
            filesRead.get(),
            filesSkipped.get(),
            chunksIndexed.get(),
            chunksCached.get(),
            relationshipsLinked.get());
    log.info(
        "Indexed {} chunks ({} cached) and {} relationships from {} files ({} skipped) for {}",
        result.chunksIndexed(),
        result.chunksCached(),
        result.relationshipsLinked(),
        result.filesRead(),
        result.filesSkipped(),
        sourceId);
    return result;
  }
}
