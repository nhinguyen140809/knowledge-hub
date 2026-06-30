package com.knowledgehub.retrieval.infrastructure.web;

import com.knowledgehub.retrieval.domain.Query;
import com.knowledgehub.retrieval.domain.QueryParams;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Request body for a retrieval query: the free-text {@code text} plus optional knobs. Mirrors
 * {@link Query}/{@link QueryParams} so the web layer never exposes the domain types directly.
 *
 * @param text the free-text query (required, non-blank)
 * @param topK maximum number of results, or {@code null} for the configured default
 * @param ref restrict to a version/branch, or {@code null} for the canonical ref
 * @param type restrict to a data type ({@code code}/{@code doc}/{@code requirement}/{@code commit})
 */
public record QueryRequest(@NotBlank String text, @Positive Integer topK, String ref, String type) {

  Query toQuery() {
    return new Query(text, new QueryParams(topK, ref, type));
  }
}
