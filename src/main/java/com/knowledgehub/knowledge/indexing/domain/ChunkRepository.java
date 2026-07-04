package com.knowledgehub.knowledge.indexing.domain;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Persists chunks and their structural relationships in the graph. Writes are idempotent by {@code
 * chunkId} (re-indexing the same content does not create duplicates). The vector for each chunk
 * lives in the {@code VectorStorePort}, joined by {@code chunkId}.
 */
public interface ChunkRepository {

  /**
   * Upserts the given chunks, linking each to the file it was cut from and, when it belongs to one,
   * to its code entity.
   */
  void upsertAll(List<Chunk> chunks);

  /**
   * Returns which of the given content hashes are already indexed for the source — the dedup/cache
   * lookup that lets unchanged chunks skip re-embedding.
   *
   * @param sourceId the source to scope the lookup to
   * @param contentHashes the hashes to check
   * @return the subset already present (empty if none)
   */
  Set<String> existingContentHashes(String sourceId, Collection<String> contentHashes);

  /** Removes the given chunks (eviction on update). */
  void deleteByChunkIds(List<String> chunkIds);

  /** Removes all chunks of a source (eviction on source delete). */
  void deleteBySource(String sourceId);
}
