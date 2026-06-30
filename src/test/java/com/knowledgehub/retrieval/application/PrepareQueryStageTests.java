package com.knowledgehub.retrieval.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.knowledgehub.knowledge.domain.EmbeddingPort;
import com.knowledgehub.knowledge.domain.Filter;
import com.knowledgehub.retrieval.domain.Query;
import org.junit.jupiter.api.Test;

class PrepareQueryStageTests {

  private final EmbeddingPort embeddingPort = mock(EmbeddingPort.class);
  private final PrepareQueryStage stage = new PrepareQueryStage(embeddingPort);

  @Test
  void embedsTheTextAndExtractsKeywordsWithoutStopWords() {
    when(embeddingPort.embed("How does the Greeter greet")).thenReturn(new float[] {1f, 2f, 3f});
    RetrievalContext context =
        new RetrievalContext(Query.of("How does the Greeter greet"), Filter.unrestricted());

    stage.apply(context);

    assertThat(context.embedding()).containsExactly(1f, 2f, 3f);
    // how/does/the are stop words; the rest survive, lower-cased and de-duplicated in order.
    assertThat(context.keywords()).containsExactly("greeter", "greet");
  }

  @Test
  void dropsSingleCharacterTokens() {
    when(embeddingPort.embed("a b Greeter")).thenReturn(new float[] {0f});
    RetrievalContext context = new RetrievalContext(Query.of("a b Greeter"), Filter.unrestricted());

    stage.apply(context);

    assertThat(context.keywords()).containsExactly("greeter");
  }
}
