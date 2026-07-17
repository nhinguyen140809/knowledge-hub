package com.knowledgehub.knowledge.indexing.domain;

import com.knowledgehub.knowledge.analysis.domain.CodeEntity;
import java.util.List;

/**
 * Persists code entities and their structural hierarchy. Writes are idempotent by {@code entityId}.
 * Semantic relationships between entities are managed separately by knowledge linking.
 */
public interface CodeEntityRepository {

  /**
   * Upserts the given entities, preserving their structural hierarchy: each is attached to the file
   * that declares it (top-level) or to its enclosing parent entity (members).
   */
  void upsertAll(List<CodeEntity> entities);

  /** Removes all entities of a source (eviction on source delete). */
  void deleteBySource(String sourceId);
}
