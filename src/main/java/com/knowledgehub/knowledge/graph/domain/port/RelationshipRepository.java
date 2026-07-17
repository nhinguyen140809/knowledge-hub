package com.knowledgehub.knowledge.graph.domain.port;

import com.knowledgehub.knowledge.domain.Relationship;
import java.util.List;

/**
 * Persists graph relationships. Writes are idempotent, keyed by {@code (from, to, type)} so
 * re-linking unchanged content never duplicates an edge.
 */
public interface RelationshipRepository {

  /**
   * Upserts every relationship, creating the typed edge between the two entity ids and storing its
   * confidence and evidence.
   *
   * @param relationships the relationships to write
   */
  void upsertAll(List<Relationship> relationships);

  /**
   * Removes relationships created for a source (those whose source-end entity belongs to it), so a
   * re-link or sync can rebuild them cleanly.
   *
   * @param sourceId the source whose relationships to drop
   */
  void deleteBySource(String sourceId);
}
