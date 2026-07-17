package com.knowledgehub.knowledge.domain.port;

import java.util.List;

/**
 * Turns text into embedding vectors. One embedding model is used for both indexing and querying —
 * changing it requires a full re-index. The implementation lives in infrastructure and is selected
 * by config.
 */
public interface EmbeddingPort {

  /** Embeds a single text. */
  float[] embed(String text);

  /** Embeds many texts in one call (batched to cut cost/latency). */
  List<float[]> embedBatch(List<String> texts);

  /** The dimension of the vectors this model produces (must match the vector index). */
  int dimension();
}
