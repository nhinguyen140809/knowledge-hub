package com.knowledgehub.retrieval.domain;

import java.util.Objects;

/**
 * A free-text retrieval request plus its optional parameters. The system does not interpret intent;
 * the text is used verbatim as the input to embed (semantic) and to tokenize (keyword), and the
 * agent on the other side is responsible for what the query means.
 *
 * @param text the free-text query (never blank)
 * @param params optional knobs (top-k, ref, type); never {@code null}
 */
public record Query(String text, QueryParams params) {

  public Query {
    Objects.requireNonNull(text, "text");
    if (text.isBlank()) {
      throw new IllegalArgumentException("query text must not be blank");
    }
    if (params == null) {
      params = QueryParams.defaults();
    }
  }

  /** A query with default parameters. */
  public static Query of(String text) {
    return new Query(text, QueryParams.defaults());
  }
}
