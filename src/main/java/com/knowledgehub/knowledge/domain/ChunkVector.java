package com.knowledgehub.knowledge.domain;

import java.util.Map;

/**
 * A chunk's embedding plus the metadata needed to persist and later filter it. Produced by the
 * embed stage and consumed by {@link VectorStorePort#upsert}.
 *
 * @param chunkId the chunk's stable id (links the vector back to its graph node)
 * @param embedding the dense embedding vector
 * @param metadata indexable metadata (at least {@code source_id}; also path, ref, line range…)
 */
public record ChunkVector(String chunkId, float[] embedding, Map<String, Object> metadata) {

  public ChunkVector {
    metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
  }
}
