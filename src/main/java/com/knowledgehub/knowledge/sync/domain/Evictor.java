package com.knowledgehub.knowledge.sync.domain;

import java.util.Collection;

/**
 * Removes stale knowledge from both stores at once - the vector point in Qdrant and the node/edges
 * in Neo4j, keyed by {@code chunk_id} - so a deleted or shrunken file never leaves a hit the query
 * can still return. Keeping the two stores in step is the whole point: a vector with no node (or
 * the reverse) would answer from dead data.
 */
public interface Evictor {

  /**
   * Evicts the given files entirely: their chunks, code entities and the file node, plus every
   * vector and edge attached to them.
   *
   * @param sourceId the source the files belong to
   * @param paths the file paths to remove
   */
  void evictFiles(String sourceId, Collection<String> paths);

  /**
   * Evicts the chunks of one file that are no longer present after a re-index - the chunks whose
   * ids are not in {@code keepChunkIds} - leaving the unchanged ones (and their vectors) in place.
   *
   * @param sourceId the source the file belongs to
   * @param path the file path
   * @param keepChunkIds the chunk ids the file still has
   */
  void retainChunks(String sourceId, String path, Collection<String> keepChunkIds);

  /**
   * Evicts everything belonging to a source (used when the source itself is removed).
   *
   * @param sourceId the source to purge
   */
  void evictSource(String sourceId);
}
