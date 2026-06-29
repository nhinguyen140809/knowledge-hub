package com.knowledgehub.knowledge.indexing.application;

import com.knowledgehub.knowledge.indexing.domain.Chunker;
import com.knowledgehub.knowledge.indexing.domain.ChunkingResult;
import com.knowledgehub.shared.pipeline.Stage;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * First filter: picks the first {@link Chunker} that supports the artifact (Strategy by {@code
 * supports}/order) and cuts it into chunks plus code entities. An artifact no chunker handles, or
 * that fails to chunk, is marked skipped so the rest of the run continues (NFR-6.1).
 */
@Component
class ChunkStage implements Stage<IndexingContext> {

  private static final Logger log = LoggerFactory.getLogger(ChunkStage.class);

  private final List<Chunker> chunkers;

  ChunkStage(List<Chunker> chunkers) {
    this.chunkers = chunkers;
  }

  @Override
  public IndexingContext apply(IndexingContext context) {
    if (context.isSkipped()) {
      return context;
    }
    Chunker chunker =
        chunkers.stream().filter(c -> c.supports(context.artifact())).findFirst().orElse(null);
    if (chunker == null) {
      context.markSkipped("no chunker for " + context.artifact().mediaType());
      log.debug(
          "No chunker for artifact {} ({})",
          context.artifact().path(),
          context.artifact().mediaType());
      return context;
    }
    try {
      ChunkingResult result = chunker.chunk(context.artifact(), context.config());
      context.setChunked(result.chunks(), result.codeEntities());
    } catch (RuntimeException e) {
      context.markSkipped("chunking failed: " + e);
      log.warn("Skipping artifact {}: {}", context.artifact().path(), e.toString());
    }
    return context;
  }
}
