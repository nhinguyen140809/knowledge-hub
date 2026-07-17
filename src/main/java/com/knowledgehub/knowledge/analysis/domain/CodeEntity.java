package com.knowledgehub.knowledge.analysis.domain;

import com.knowledgehub.shared.id.IdFactory;
import java.util.Objects;

/**
 * A named code construct (class, method, field…) extracted from a source file. An entity is the
 * unit that structural and semantic relationships between code are expressed over. The {@code
 * entityId} is derived from {@code (sourceId, path, qualifiedName)} so it is stable across edits to
 * the body, letting a re-extracted entity replace its prior version in place.
 *
 * @param entityId stable, identity-derived id
 * @param sourceId the source this entity came from
 * @param fileId stable id of the declaring file
 * @param parentEntityId enclosing entity (a type for a method/field), or {@code null} when the file
 *     declares it directly
 * @param level the entity's granularity
 * @param name simple name
 * @param qualifiedName fully-qualified name (package + enclosing types + member), used to resolve
 *     references from other files/sources to this entity
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
    String qualifiedName,
    String signature,
    int lineStart,
    int lineEnd) {

  public CodeEntity {
    Objects.requireNonNull(entityId, "entityId");
    Objects.requireNonNull(sourceId, "sourceId");
    Objects.requireNonNull(fileId, "fileId");
    Objects.requireNonNull(level, "level");
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(qualifiedName, "qualifiedName");
    if (lineStart < 1 || lineEnd < lineStart) {
      throw new IllegalArgumentException("invalid line range: " + lineStart + ".." + lineEnd);
    }
  }

  /**
   * Derives the stable id of the entity with these coordinates. Content-independent (so it survives
   * body edits) and computable from parts alone — a reference target's id can be derived before the
   * entity itself is loaded, which is how cross-file links resolve.
   */
  public static String deriveId(String sourceId, String path, String qualifiedName) {
    return IdFactory.stableId(sourceId, path, qualifiedName);
  }
}
