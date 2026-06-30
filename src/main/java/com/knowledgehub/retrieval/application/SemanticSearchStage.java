package com.knowledgehub.retrieval.application;

import com.knowledgehub.knowledge.domain.VectorStorePort;
import com.knowledgehub.shared.config.AppProperties;
import com.knowledgehub.shared.pipeline.Stage;
import org.springframework.stereotype.Component;

/**
 * The semantic retrieval path: searches the vector store with the query embedding and the ACL
 * pre-filter. One of three search stages that fan out in parallel; it writes only its own hit list,
 * so it shares the context safely with the keyword stage.
 */
@Component
class SemanticSearchStage implements Stage<RetrievalContext> {

  private final VectorStorePort vectorStore;
  private final int candidateK;

  SemanticSearchStage(VectorStorePort vectorStore, AppProperties properties) {
    this.vectorStore = vectorStore;
    this.candidateK = properties.retrieval().candidateK();
  }

  @Override
  public RetrievalContext apply(RetrievalContext context) {
    context.setSemanticHits(
        vectorStore.search(context.embedding(), candidateK, context.aclFilter()));
    return context;
  }
}
