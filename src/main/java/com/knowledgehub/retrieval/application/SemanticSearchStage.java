package com.knowledgehub.retrieval.application;

import com.knowledgehub.knowledge.domain.EmbeddingPort;
import com.knowledgehub.knowledge.domain.VectorStorePort;
import com.knowledgehub.shared.config.AppProperties;
import com.knowledgehub.shared.pipeline.Stage;
import org.springframework.stereotype.Component;

/**
 * The semantic retrieval path: embeds the query and searches the vector store with the ACL
 * pre-filter. Embedding happens here, not in query preparation, so that if the embedding provider
 * fails this whole path degrades to empty (caught by the orchestrator) while the keyword and graph
 * paths still serve. Runs in parallel with the keyword stage and writes only its own hit list.
 */
@Component
class SemanticSearchStage implements Stage<RetrievalContext> {

  private final VectorStorePort vectorStore;
  private final EmbeddingPort embeddingPort;
  private final int candidateK;

  SemanticSearchStage(
      VectorStorePort vectorStore, EmbeddingPort embeddingPort, AppProperties properties) {
    this.vectorStore = vectorStore;
    this.embeddingPort = embeddingPort;
    this.candidateK = properties.retrieval().candidateK();
  }

  @Override
  public RetrievalContext apply(RetrievalContext context) {
    float[] embedding = embeddingPort.embed(context.query().text());
    context.setSemanticHits(vectorStore.search(embedding, candidateK, context.aclFilter()));
    return context;
  }
}
