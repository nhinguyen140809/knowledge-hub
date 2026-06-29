package com.knowledgehub.knowledge.indexing.application;

import com.knowledgehub.knowledge.domain.VectorStorePort;
import com.knowledgehub.knowledge.indexing.domain.ChunkRepository;
import com.knowledgehub.knowledge.indexing.domain.CodeEntityRepository;
import com.knowledgehub.shared.pipeline.Stage;
import org.springframework.stereotype.Component;

/**
 * Final filter: persists the new chunks. The vector goes to the vector store and the {@code :Chunk}
 * node (with provenance and structural links) to the graph, joined by {@code chunk_id}; code
 * entities are upserted first so {@code CHUNK_OF} targets exist. Both writes are idempotent by
 * content-derived id, so a partial failure is safe to retry — there is no cross-store transaction.
 */
@Component
class StoreStage implements Stage<IndexingContext> {

  private final VectorStorePort vectorStore;
  private final ChunkRepository chunks;
  private final CodeEntityRepository entities;

  StoreStage(VectorStorePort vectorStore, ChunkRepository chunks, CodeEntityRepository entities) {
    this.vectorStore = vectorStore;
    this.chunks = chunks;
    this.entities = entities;
  }

  @Override
  public IndexingContext apply(IndexingContext context) {
    if (context.isSkipped() || context.newChunks().isEmpty()) {
      return context;
    }
    entities.upsertAll(context.entities());
    vectorStore.upsert(context.vectors());
    chunks.upsertAll(context.newChunks());
    return context;
  }
}
