package com.knowledgehub.knowledge.indexing.domain;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Persists chunk nodes and their structural relationships in the graph. Writes are idempotent by
 * {@code chunkId} (re-indexing the same content does not create duplicates). The vector for each
 * chunk lives in the {@code VectorStorePort}, joined by {@code chunkId}.
 */
public interface ChunkRepository {

  /**
   * Upserts the given chunks as {@code :Chunk} nodes, each linked {@code PART_OF} its {@code :File}
   * and, when it belongs to one, {@code CHUNK_OF} its {@code :CodeEntity}.
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

  /** Removes the given chunk nodes (eviction on update). */
  void deleteByChunkIds(List<String> chunkIds);

  /** Removes all chunk nodes of a source (eviction on source delete). */
  void deleteBySource(String sourceId);
}
