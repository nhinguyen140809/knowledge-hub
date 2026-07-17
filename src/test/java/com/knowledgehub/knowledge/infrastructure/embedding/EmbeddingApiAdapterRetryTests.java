package com.knowledgehub.knowledge.infrastructure.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.knowledgehub.knowledge.domain.port.EmbeddingPort;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
@TestPropertySource(
    properties = {"app.embedding.retry-max-attempts=3", "app.embedding.retry-backoff-ms=1"})
class EmbeddingApiAdapterRetryTests {

  @Configuration
  @EnableRetry
  static class TestConfig {
    @Bean
    EmbeddingModel embeddingModel() {
      return mock(EmbeddingModel.class);
    }

    @Bean
    EmbeddingApiAdapter embeddingApiAdapter(EmbeddingModel model) {
      return new EmbeddingApiAdapter(model, 1000);
    }
  }

  @Autowired private EmbeddingModel model;
  @Autowired private EmbeddingPort adapter;

  @Test
  void retriesThreeTimesThenFailsWhenTheProviderKeepsErroring() {
    when(model.embed(List.of("x"))).thenThrow(new RuntimeException("provider 503"));

    assertThatThrownBy(() -> adapter.embedBatch(List.of("x")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("after retries");

    verify(model, times(3)).embed(List.of("x"));
  }

  @Test
  void succeedsAfterATransientFailure() {
    Mockito.reset(model);
    when(model.embed(List.of("y")))
        .thenThrow(new RuntimeException("transient"))
        .thenReturn(List.of(new float[] {1f}));

    List<float[]> result = adapter.embedBatch(List.of("y"));

    assertThat(result).hasSize(1);
    verify(model, times(2)).embed(List.of("y"));
  }
}
