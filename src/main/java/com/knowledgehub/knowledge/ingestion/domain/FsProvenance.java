package com.knowledgehub.knowledge.ingestion.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Provenance for a unit ingested from a filesystem source. There is no version control, so the
 * version is identified by {@code contentHash} alone.
 */
public record FsProvenance(String sourceId, String path, String contentHash, Instant indexedAt)
    implements Provenance {

  public FsProvenance {
    Objects.requireNonNull(sourceId, "sourceId");
    Objects.requireNonNull(path, "path");
    Objects.requireNonNull(contentHash, "contentHash");
    Objects.requireNonNull(indexedAt, "indexedAt");
  }
}
