package com.knowledgehub.retrieval.infrastructure.web;

import com.knowledgehub.retrieval.domain.Hit;
import com.knowledgehub.retrieval.domain.HitMetadata;
import com.knowledgehub.retrieval.domain.RankedResult;
import java.util.List;

/**
 * JSON response for a query: the ranked hits with their scores and metadata, plus the canonical-ref
 * flag. A structured shape an agent can consume - each hit carries enough to cite the source (path,
 * line range, ref/commit).
 *
 * @param hits the ranked hits, best-first
 * @param servedFromCanonicalRef true when the requested ref was not indexed and the canonical ref
 *     was served instead
 */
public record RankedResultResponse(List<HitResponse> hits, boolean servedFromCanonicalRef) {

  static RankedResultResponse from(RankedResult result) {
    List<HitResponse> hits = result.hits().stream().map(HitResponse::from).toList();
    return new RankedResultResponse(hits, result.servedFromCanonicalRef());
  }

  /**
   * One ranked hit.
   *
   * @param id the matched chunk id or entity id
   * @param relevanceScore the fused relevance score
   * @param metadata where the hit lives and how it was found
   */
  public record HitResponse(String id, double relevanceScore, MetadataResponse metadata) {

    static HitResponse from(Hit hit) {
      return new HitResponse(hit.id(), hit.relevanceScore(), MetadataResponse.from(hit.metadata()));
    }
  }

  /**
   * Hit metadata, including the line position and version a caller needs to fetch and cite it.
   *
   * @param kind {@code chunk} or {@code entity}
   * @param sourceId the source the hit belongs to
   * @param path the file path within the source
   * @param lineStart first line of the hit, or {@code null}
   * @param lineEnd last line of the hit, or {@code null}
   * @param type the data type, or {@code null}
   * @param ref the version/branch, or {@code null}
   * @param indexedAt when indexed (ISO-8601), or {@code null}
   * @param commitSha the commit, or {@code null}
   * @param viaPath relationship types traversed for a graph-expanded hit
   */
  public record MetadataResponse(
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

    static MetadataResponse from(HitMetadata metadata) {
      return new MetadataResponse(
          metadata.kind(),
          metadata.sourceId(),
          metadata.path(),
          metadata.lineStart(),
          metadata.lineEnd(),
          metadata.type(),
          metadata.ref(),
          metadata.indexedAt(),
          metadata.commitSha(),
          metadata.viaPath());
    }
  }
}
