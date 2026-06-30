package com.knowledgehub.retrieval.domain;

import com.knowledgehub.knowledge.domain.Filter;
import com.knowledgehub.knowledge.domain.ScoredId;
import java.util.List;

/**
 * Lexical (BM25) search over indexed text. Complements semantic search: it catches exact
 * identifiers and rare terms an embedding blurs. The {@code filter} carries {@code allowedSources}
 * and is applied as a hard pre-filter, so a disallowed source is never even scored.
 */
public interface KeywordSearchPort {

  /**
   * Returns the top-{@code k} ids matching the given keywords, best-first.
   *
   * @param keywords the query tokens (already split and stop-word filtered)
   * @param k maximum number of results
   * @param filter source/ref/type restrictions applied as a pre-filter
   */
  List<ScoredId> search(List<String> keywords, int k, Filter filter);
}
