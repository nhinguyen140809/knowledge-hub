package com.knowledgehub.knowledge.ingestion.domain;

import java.time.Instant;
import java.util.Optional;

/**
 * Origin metadata attached to every ingested unit. The common fields ({@code sourceId}, {@code
 * path}, {@code contentHash}, {@code indexedAt}) trace a unit back to its source; Git sources carry
 * extra version coordinates (see {@link GitProvenance}), filesystem sources identify versions by
 * content hash alone (see {@link FsProvenance}).
 */
public sealed interface Provenance permits GitProvenance, FsProvenance {

  /** The id of the source this unit came from. */
  String sourceId();

  /** The path of the unit within its source (repo-relative or folder-relative). */
  String path();

  /** Lowercase hex SHA-256 of the unit's content. */
  String contentHash();

  /** When the unit was ingested (UTC). */
  Instant indexedAt();

  /**
   * Version-control coordinates of this unit, for sources that track them (Git); empty for sources
   * that identify versions by content alone (filesystem). Lets callers read {@code ref}/{@code
   * commitSha} without knowing the concrete provenance type.
   */
  default Optional<VersionRef> version() {
    return Optional.empty();
  }
}
