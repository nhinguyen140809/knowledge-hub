package com.knowledgehub.knowledge.ingestion.domain;

import java.util.Objects;

/**
 * Version-control coordinates of an ingested unit: the {@code ref} (branch/tag) it was read on and
 * the {@code commitSha} it resolved to. Present only for sources that track versions (Git).
 */
public record VersionRef(String ref, String commitSha) {

  public VersionRef {
    Objects.requireNonNull(ref, "ref");
    Objects.requireNonNull(commitSha, "commitSha");
  }
}
