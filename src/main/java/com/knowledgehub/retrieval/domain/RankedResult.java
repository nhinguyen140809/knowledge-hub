package com.knowledgehub.retrieval.domain;

import java.util.List;

/**
 * The ordered answer to a query: hits best-first, plus the flags an agent needs to trust them.
 *
 * @param hits the ranked hits, highest score first
 * @param servedFromCanonicalRef true when the requested ref was not indexed and the result was
 *     served from the canonical ref instead (recorded so the caller knows it asked for one version
 *     and got another)
 */
public record RankedResult(List<Hit> hits, boolean servedFromCanonicalRef) {

  public RankedResult {
    hits = hits == null ? List.of() : List.copyOf(hits);
  }

  /** An empty result served from the canonical ref. */
  public static RankedResult empty() {
    return new RankedResult(List.of(), false);
  }
}
