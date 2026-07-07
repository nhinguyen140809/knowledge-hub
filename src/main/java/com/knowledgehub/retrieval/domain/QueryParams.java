package com.knowledgehub.retrieval.domain;

/**
 * Optional knobs on a query. All are nullable so a caller can omit them; the service fills a
 * missing {@code topK} from configuration and treats a missing {@code sourceId}/{@code ref}/{@code
 * type} as no restriction.
 *
 * @param topK maximum number of results to return, or {@code null} to use the configured default
 * @param sourceId restrict to a single source, or {@code null} for every readable source. A ref is
 *     only meaningful within one source, so this is the natural companion of {@code ref}. Always
 *     intersected with the caller's readable set — it can narrow the scope, never widen it.
 * @param ref restrict to a version/branch, or {@code null} for the canonical ref
 * @param type restrict to a data type ({@code code}, {@code doc}, {@code requirement}, {@code
 *     commit}), or {@code null} for any
 */
public record QueryParams(Integer topK, String sourceId, String ref, String type) {

  /**
   * Normalizes a blank {@code sourceId}/{@code ref}/{@code type} to {@code null} — every caller
   * (REST body, MCP tool params) means "no restriction" by either omitting the field or sending it
   * blank, but only {@code null} is special-cased downstream. Without this, a caller that sends
   * {@code ""} instead of omitting the field (e.g. Swagger UI's "Try it out", which pre-fills
   * optional string fields empty) would filter on an empty string that matches nothing and silently
   * get zero results.
   */
  public QueryParams {
    sourceId = blankToNull(sourceId);
    ref = blankToNull(ref);
    type = blankToNull(type);
  }

  private static String blankToNull(String value) {
    return (value == null || value.isBlank()) ? null : value;
  }

  /**
   * Parameters with nothing set - default top-k, every readable source, canonical ref, any type.
   */
  public static QueryParams defaults() {
    return new QueryParams(null, null, null, null);
  }
}
