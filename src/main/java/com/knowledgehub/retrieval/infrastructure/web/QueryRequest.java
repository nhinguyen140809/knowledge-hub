package com.knowledgehub.retrieval.infrastructure.web;

import com.knowledgehub.retrieval.domain.Query;
import com.knowledgehub.retrieval.domain.QueryParams;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Request body for a retrieval query: the free-text {@code text} plus optional knobs. Mirrors
 * {@link Query}/{@link QueryParams} so the web layer never exposes the domain types directly.
 *
 * @param text the free-text query (required, non-blank)
 * @param topK maximum number of results, or {@code null} for the configured default
 * @param sourceId restrict to a single source (narrows within the caller's readable set)
 * @param ref restrict to a version/branch, or {@code null} for the canonical ref
 * @param type restrict to a data type ({@code code}/{@code doc}/{@code requirement}/{@code commit})
 */
public record QueryRequest(
    @Schema(description = "Free-text query", example = "How is the retrieval cache keyed?")
        @NotBlank
        String text,
    @Schema(description = "Maximum number of results; defaults to the server's configured top-k")
        @Positive
        Integer topK,
    @Schema(
            description = "Restrict to a single source id (narrows within your readable sources)",
            example = "docs-service")
        String sourceId,
    @Schema(description = "Restrict to a version/branch ref", example = "main") String ref,
    @Schema(
            description = "Restrict to a data type",
            allowableValues = {"code", "doc", "requirement", "commit"},
            example = "doc")
        String type) {

  Query toQuery() {
    return new Query(
        text, new QueryParams(topK, blankToNull(sourceId), blankToNull(ref), blankToNull(type)));
  }

  /**
   * A blank {@code sourceId}/{@code ref}/{@code type} means the same thing as an absent one ("don't
   * restrict"), but {@link QueryParams} and every filter downstream only special-case {@code null}
   * — a client that sends {@code ""} instead of omitting the field (e.g. Swagger UI's "Try it out",
   * which pre-fills optional string fields empty) would otherwise filter on an empty string that
   * matches nothing and silently get zero results.
   */
  private static String blankToNull(String value) {
    return (value == null || value.isBlank()) ? null : value;
  }
}
