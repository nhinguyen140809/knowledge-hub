package com.knowledgehub.retrieval.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.knowledgehub.knowledge.domain.EmbeddingPort;
import com.knowledgehub.knowledge.domain.Filter;
import com.knowledgehub.knowledge.domain.ScoredId;
import com.knowledgehub.knowledge.domain.VectorStorePort;
import com.knowledgehub.retrieval.domain.Query;
import com.knowledgehub.shared.config.AppProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

class SemanticSearchStageTests {

  private final VectorStorePort vectorStore = mock(VectorStorePort.class);
  private final EmbeddingPort embeddingPort = mock(EmbeddingPort.class);
  private final SemanticSearchStage stage =
      new SemanticSearchStage(
          vectorStore, embeddingPort, new AppProperties(null, null, null, null, null, null));

  @Test
  void embedsTheQueryHereThenSearchesTheVectorStore() {
    when(embeddingPort.embed("greeter")).thenReturn(new float[] {1f, 2f});
    when(vectorStore.search(any(), anyInt(), any())).thenReturn(List.of(new ScoredId("a", 0.9)));
    RetrievalContext context = new RetrievalContext(Query.of("greeter"), Filter.unrestricted());

    stage.apply(context);

    assertThat(context.semanticHits()).extracting(ScoredId::chunkId).containsExactly("a");
    // Embedding is owned by this path, not query preparation.
    verify(embeddingPort).embed("greeter");
  }
}
