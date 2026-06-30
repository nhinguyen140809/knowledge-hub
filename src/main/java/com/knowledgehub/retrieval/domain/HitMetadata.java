package com.knowledgehub.retrieval.domain;

import java.util.List;

/**
 * Where a hit lives and how it was found - enough for an agent to cite it and fetch it back. Loaded
 * from the graph after fusion and filtering, so only surviving hits cost a lookup.
 *
 * @param kind whether the hit is a {@code chunk} or a code {@code entity}
 * @param sourceId the source the hit belongs to
 * @param path the file path within the source
 * @param lineStart first line of the hit (1-based), or {@code null} if unknown
 * @param lineEnd last line of the hit (1-based), or {@code null} if unknown
 * @param type the data type ({@code code}/{@code doc}), or {@code null}
 * @param ref the version/branch the hit was indexed at, or {@code null}
 * @param indexedAt when the hit was indexed (ISO-8601), or {@code null}
 * @param commitSha the commit the hit was indexed at, or {@code null}
 * @param viaPath the relationship types traversed to reach a graph-expanded hit, empty otherwise
 */
public record HitMetadata(
    String kind,
    String sourceId,
    String path,
    Integer lineStart,
    Integer lineEnd,
    String type,
    String ref,
    String indexedAt,
    String commitSha,
    List<String> viaPath) {

  public HitMetadata {
    viaPath = viaPath == null ? List.of() : List.copyOf(viaPath);
  }
}
