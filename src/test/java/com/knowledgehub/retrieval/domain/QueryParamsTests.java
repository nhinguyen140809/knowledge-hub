package com.knowledgehub.retrieval.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class QueryParamsTests {

  @Test
  void blankOptionalFieldsAreNormalizedToNull() {
    QueryParams params = new QueryParams(5, "", "", "");

    assertThat(params.sourceId()).isNull();
    assertThat(params.ref()).isNull();
    assertThat(params.type()).isNull();
    assertThat(params.topK()).isEqualTo(5);
  }

  @Test
  void nonBlankFieldsPassThroughUnchanged() {
    QueryParams params = new QueryParams(5, "src-a", "main", "code");

    assertThat(params.sourceId()).isEqualTo("src-a");
    assertThat(params.ref()).isEqualTo("main");
    assertThat(params.type()).isEqualTo("code");
  }
}
