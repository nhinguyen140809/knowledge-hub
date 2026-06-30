package com.knowledgehub.retrieval.domain;

/**
 * Optional knobs on a query. All are nullable so a caller can omit them; the service fills a
 * missing {@code topK} from configuration and treats a missing {@code ref}/{@code type} as no
 * restriction.
 *
 * @param topK maximum number of results to return, or {@code null} to use the configured default
 * @param ref restrict to a version/branch, or {@code null} for the canonical ref
 * @param type restrict to a data type ({@code code}, {@code doc}, {@code requirement}, {@code
 *     commit}), or {@code null} for any
 */
public record QueryParams(Integer topK, String ref, String type) {

  /** Parameters with nothing set - default top-k, canonical ref, any type. */
  public static QueryParams defaults() {
    return new QueryParams(null, null, null);
  }
}
