package com.knowledgehub.retrieval.application;

import com.knowledgehub.retrieval.domain.KeywordSearchPort;
import com.knowledgehub.shared.config.AppProperties;
import com.knowledgehub.shared.pipeline.Stage;
import org.springframework.stereotype.Component;

/**
 * The keyword (BM25) retrieval path: searches the full-text indexes with the prepared keywords and
 * the ACL pre-filter. Runs in parallel with the semantic stage and writes only its own hit list.
 */
@Component
class KeywordSearchStage implements Stage<RetrievalContext> {

  private final KeywordSearchPort keywordSearch;
  private final int candidateK;

  KeywordSearchStage(KeywordSearchPort keywordSearch, AppProperties properties) {
    this.keywordSearch = keywordSearch;
    this.candidateK = properties.retrieval().candidateK();
  }

  @Override
  public RetrievalContext apply(RetrievalContext context) {
    if (context.keywords().isEmpty()) {
      return context;
    }
    context.setKeywordHits(
        keywordSearch.search(context.keywords(), candidateK, context.aclFilter()));
    return context;
  }
}
