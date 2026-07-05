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
 * to paths before content is read. {@code name} and {@code description} are optional human-facing
 * metadata — a short label and a longer explanation of what the source holds — that carry no
 * behaviour; a blank value is normalized to absent.
 */
public final class Source {

  private final String sourceId;
  private final SourceType type;
  private final String uriOrPath;
  private final String ref;
  private final List<String> include;
  private final List<String> ignore;
  private final String name;
  private final String description;

  /** Convenience constructor for a source with no human-facing metadata. */
  public Source(
      String sourceId,
      SourceType type,
      String uriOrPath,
      String ref,
      List<String> include,
      List<String> ignore) {
    this(sourceId, type, uriOrPath, ref, include, ignore, null, null);
  }

  public Source(
      String sourceId,
      SourceType type,
      String uriOrPath,
      String ref,
      List<String> include,
      List<String> ignore,
      String name,
      String description) {
    this.sourceId = requireText(sourceId, "sourceId");
    this.type = Objects.requireNonNull(type, "type");
    this.uriOrPath = requireText(uriOrPath, "uriOrPath");
    if (type == SourceType.FS && ref != null) {
      throw new IllegalArgumentException("ref is only valid for a GIT source");
    }
    this.ref = ref;
    this.include = include == null ? List.of() : List.copyOf(include);
    this.ignore = ignore == null ? List.of() : List.copyOf(ignore);
    this.name = blankToNull(name);
    this.description = blankToNull(description);
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

  /** The human-facing label for this source, or empty if none was given. */
  public Optional<String> name() {
    return Optional.ofNullable(name);
  }

  /** A longer human-facing description of what this source holds, or empty if none was given. */
  public Optional<String> description() {
    return Optional.ofNullable(description);
  }

  /**
   * Returns a copy with new editable configuration — the Git {@code ref} and the include/ignore
   * globs — while the identity ({@code sourceId}), {@code type}, location ({@code uriOrPath}) and
   * metadata ({@code name}/{@code description}) stay unchanged. The same construction-time
   * invariants apply (e.g. {@code ref} only for Git).
   */
  public Source withConfig(String ref, List<String> include, List<String> ignore) {
    return new Source(sourceId, type, uriOrPath, ref, include, ignore, name, description);
  }

  /**
   * Returns a copy with new human-facing metadata — the {@code name} and {@code description} —
   * while identity, location and all ingestion configuration stay unchanged. A blank value clears
   * that field.
   */
  public Source withMetadata(String name, String description) {
    return new Source(sourceId, type, uriOrPath, ref, include, ignore, name, description);
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value;
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
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
