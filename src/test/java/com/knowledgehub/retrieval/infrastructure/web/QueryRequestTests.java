package com.knowledgehub.retrieval.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class QueryRequestTests {

  @Test
  void blankOptionalFieldsAreTreatedAsAbsent() {
    QueryRequest request = new QueryRequest("how does routing work", null, "", "", "");

    var params = request.toQuery().params();

    assertThat(params.sourceId()).isNull();
    assertThat(params.ref()).isNull();
    assertThat(params.type()).isNull();
  }

  @Test
  void nonBlankOptionalFieldsPassThrough() {
    QueryRequest request = new QueryRequest("how does routing work", 5, "src-a", "main", "code");

    var params = request.toQuery().params();

    assertThat(params.topK()).isEqualTo(5);
    assertThat(params.sourceId()).isEqualTo("src-a");
    assertThat(params.ref()).isEqualTo("main");
    assertThat(params.type()).isEqualTo("code");
  }
}
