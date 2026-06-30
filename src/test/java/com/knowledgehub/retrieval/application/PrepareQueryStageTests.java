package com.knowledgehub.retrieval.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledgehub.knowledge.domain.Filter;
import com.knowledgehub.retrieval.domain.Query;
import org.junit.jupiter.api.Test;

class PrepareQueryStageTests {

  private final PrepareQueryStage stage = new PrepareQueryStage();

  @Test
  void extractsKeywordsWithoutStopWords() {
    RetrievalContext context =
        new RetrievalContext(Query.of("How does the Greeter greet"), Filter.unrestricted());

    stage.apply(context);

    // how/does/the are stop words; the rest survive, lower-cased and de-duplicated in order.
    assertThat(context.keywords()).containsExactly("greeter", "greet");
  }

  @Test
  void dropsSingleCharacterTokens() {
    RetrievalContext context = new RetrievalContext(Query.of("a b Greeter"), Filter.unrestricted());

    stage.apply(context);

    assertThat(context.keywords()).containsExactly("greeter");
  }
}
