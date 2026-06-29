package com.knowledgehub.knowledge.domain;

import java.util.List;

/**
 * Stores and searches chunk embeddings. The contract is <em>what</em> (upsert/search/delete), not
 * <em>how</em> — quantization, on-disk vs in-memory, and the exact filter strategy live below this
 * line in the adapter, so the vector store can be swapped with no application change. The {@code
 * filter} carries {@code allowedSources} for the ACL hard pre-filter.
 */
public interface VectorStorePort {

  /** Inserts or replaces the given chunk vectors (idempotent by {@code chunkId}). */
  void upsert(List<ChunkVector> chunks);

  /**
   * Returns the top-{@code k} chunk ids most similar to {@code query}, honouring {@code filter}.
   *
   * @param query the query embedding
   * @param k maximum number of results
   * @param filter source/ref/type restrictions applied as a pre-filter
   */
  List<ScoredId> search(float[] query, int k, Filter filter);

  /** Removes vectors for the given chunk ids (eviction on update). */
  void deleteByChunkIds(List<String> chunkIds);

  /** Removes all vectors belonging to a source (eviction on source delete). */
  void deleteBySource(String sourceId);
}
