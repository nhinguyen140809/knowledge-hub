package com.knowledgehub.knowledge.indexing.domain;

import java.util.Objects;

/**
 * A named code construct (class, method, field…) extracted from a source file. Entities are the
 * nodes that semantic relationships ({@code CALLS}, {@code IMPORTS}, …) attach to later; this phase
 * only creates them and the structural {@code DECLARES}/{@code CONTAINS} hierarchy. The {@code
 * entityId} is derived from {@code (sourceId, path, qualifiedName)} so it is stable across edits to
 * the body (idempotent upsert).
 *
 * @param entityId stable, identity-derived id
 * @param sourceId the source this entity came from
 * @param fileId stable id of the declaring file
 * @param parentEntityId enclosing entity (a type for a method/field), or {@code null} when the file
 *     declares it directly
 * @param level the entity's granularity
 * @param name simple name
 * @param signature a human-readable signature, or {@code null} when not applicable
 * @param lineStart first source line (1-based, inclusive)
 * @param lineEnd last source line (1-based, inclusive)
 */
public record CodeEntity(
    String entityId,
    String sourceId,
    String fileId,
    String parentEntityId,
    CodeEntityLevel level,
    String name,
    String signature,
    int lineStart,
    int lineEnd) {

  public CodeEntity {
    Objects.requireNonNull(entityId, "entityId");
    Objects.requireNonNull(sourceId, "sourceId");
    Objects.requireNonNull(fileId, "fileId");
    Objects.requireNonNull(level, "level");
    Objects.requireNonNull(name, "name");
    if (lineStart < 1 || lineEnd < lineStart) {
      throw new IllegalArgumentException("invalid line range: " + lineStart + ".." + lineEnd);
    }
  }
}
