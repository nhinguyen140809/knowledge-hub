package com.knowledgehub.knowledge.indexing.domain;

import java.util.List;

/**
 * Persists code-entity nodes and their structural hierarchy. Writes are idempotent by {@code
 * entityId}. Semantic relationships between entities are added later by knowledge linking.
 */
public interface CodeEntityRepository {

  /**
   * Upserts the given entities as {@code :CodeEntity} nodes, linking each {@code DECLARES} from its
   * {@code :File} (top-level) or {@code CONTAINS} from its parent entity (members).
   */
  void upsertAll(List<CodeEntity> entities);

  /** Removes all entity nodes of a source (eviction on source delete). */
  void deleteBySource(String sourceId);
}
