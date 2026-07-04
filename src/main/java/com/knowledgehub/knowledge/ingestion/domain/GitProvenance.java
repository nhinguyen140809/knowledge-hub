package com.knowledgehub.knowledge.ingestion.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Provenance for a unit ingested from a Git source. Adds the version coordinates {@code ref}
 * (branch) and {@code commitSha} on top of the common fields.
 */
public record GitProvenance(
    String sourceId,
    String path,
    String contentHash,
    Instant indexedAt,
    String ref,
    String commitSha)
    implements Provenance {

  public GitProvenance {
    Objects.requireNonNull(sourceId, "sourceId");
    Objects.requireNonNull(path, "path");
    Objects.requireNonNull(contentHash, "contentHash");
    Objects.requireNonNull(indexedAt, "indexedAt");
    Objects.requireNonNull(ref, "ref");
    Objects.requireNonNull(commitSha, "commitSha");
  }

  @Override
  public Optional<VersionRef> version() {
    return Optional.of(new VersionRef(ref, commitSha));
  }
}
