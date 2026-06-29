package com.knowledgehub.knowledge.indexing.application;

import com.knowledgehub.knowledge.indexing.domain.Chunk;
import com.knowledgehub.knowledge.indexing.domain.ChunkRepository;
import com.knowledgehub.shared.pipeline.Stage;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Second filter: drops chunks whose {@code content_hash} is already indexed for the source, so
 * unchanged content is neither re-embedded nor re-stored (the content-hash cache, FR-6.3). What
 * survives is the new/changed work the rest of the pipeline acts on.
 */
@Component
class DedupStage implements Stage<IndexingContext> {

  private final ChunkRepository chunks;

  DedupStage(ChunkRepository chunks) {
    this.chunks = chunks;
  }

  @Override
  public IndexingContext apply(IndexingContext context) {
    if (context.isSkipped() || context.chunks().isEmpty()) {
      return context;
    }
    Set<String> hashes =
        context.chunks().stream()
            .map(Chunk::contentHash)
            .collect(java.util.stream.Collectors.toSet());
    Set<String> existing = chunks.existingContentHashes(context.sourceId(), hashes);
    List<Chunk> fresh =
        context.chunks().stream().filter(c -> !existing.contains(c.contentHash())).toList();
    context.setNewChunks(fresh, context.chunks().size() - fresh.size());
    return context;
  }
}
