package com.knowledgehub.knowledge.ingestion.domain;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A configured source of knowledge — a Git repository or a filesystem folder — and the aggregate
 * root for ingestion. Identified by {@code sourceId}; two sources are equal iff their ids match.
 *
 * <p>Invariant: {@code ref} (a branch/tag/commit) is meaningful only for {@link SourceType#GIT}; it
 * must be absent for a filesystem source. {@code include}/{@code ignore} are glob patterns applied
 * to paths before content is read.
 */
public final class Source {

  private final String sourceId;
  private final SourceType type;
  private final String uriOrPath;
  private final String ref;
  private final List<String> include;
  private final List<String> ignore;

  public Source(
      String sourceId,
      SourceType type,
      String uriOrPath,
      String ref,
      List<String> include,
      List<String> ignore) {
    this.sourceId = requireText(sourceId, "sourceId");
    this.type = Objects.requireNonNull(type, "type");
    this.uriOrPath = requireText(uriOrPath, "uriOrPath");
    if (type == SourceType.FS && ref != null) {
      throw new IllegalArgumentException("ref is only valid for a GIT source");
    }
    this.ref = ref;
    this.include = include == null ? List.of() : List.copyOf(include);
    this.ignore = ignore == null ? List.of() : List.copyOf(ignore);
  }

  public String sourceId() {
    return sourceId;
  }

  public SourceType type() {
    return type;
  }

  public String uriOrPath() {
    return uriOrPath;
  }

  /**
   * The configured Git ref, or empty for a filesystem source (or a Git source using its default).
   */
  public Optional<String> ref() {
    return Optional.ofNullable(ref);
  }

  public List<String> include() {
    return include;
  }

  public List<String> ignore() {
    return ignore;
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof Source other && sourceId.equals(other.sourceId);
  }

  @Override
  public int hashCode() {
    return sourceId.hashCode();
  }

  @Override
  public String toString() {
    return "Source[" + sourceId + ", " + type + "]";
  }
}
