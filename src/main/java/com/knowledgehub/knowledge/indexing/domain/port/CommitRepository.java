package com.knowledgehub.knowledge.indexing.domain.port;

import com.knowledgehub.knowledge.ingestion.domain.CommitRecord;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Stores commits in the knowledge graph. A commit becomes a node keyed by {@code (sourceId, sha)}
 * and an edge to every indexed file its diff touched; both writes are idempotent, so re-indexing a
 * known commit changes nothing. Commits are append-only knowledge: they are never re-indexed or
 * evicted individually, only removed wholesale with their source.
 */
public interface CommitRepository {

  /** Of the given hashes, the ones already stored for the source (the dedup set). */
  Set<String> existingShas(String sourceId, Collection<String> shas);

  /**
   * Upserts the commits as graph nodes and links each to the files it modified. A changed path that
   * is not indexed (deleted since, or excluded by the source's globs) simply gets no edge.
   *
   * @param sourceId the source the commits belong to
   * @param ref the ref the history was walked from, or {@code null} if unknown
   * @param indexedAt when this indexing run happened
   * @param commits the commits to store
   */
  void upsertAll(String sourceId, String ref, Instant indexedAt, List<CommitRecord> commits);
}
