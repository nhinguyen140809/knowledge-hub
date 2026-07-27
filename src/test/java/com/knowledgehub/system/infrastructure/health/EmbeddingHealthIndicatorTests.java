package com.knowledgehub.system.infrastructure.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.knowledgehub.knowledge.domain.port.EmbeddingPort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

class EmbeddingHealthIndicatorTests {

  @Test
  void reportsUpWhenEmbeddingSucceeds() {
    EmbeddingPort embeddings = mock(EmbeddingPort.class);
    when(embeddings.embed("ping")).thenReturn(new float[] {1f});

    assertThat(new EmbeddingHealthIndicator(embeddings).health().getStatus()).isEqualTo(Status.UP);
  }

  @Test
  void reportsDownWhenEmbeddingFails() {
    EmbeddingPort embeddings = mock(EmbeddingPort.class);
    when(embeddings.embed("ping")).thenThrow(new RuntimeException("provider unreachable"));

    assertThat(new EmbeddingHealthIndicator(embeddings).health().getStatus())
        .isEqualTo(Status.DOWN);
  }

  @Test
  void cachesTheResultInsteadOfProbingEveryCall() {
    EmbeddingPort embeddings = mock(EmbeddingPort.class);
    when(embeddings.embed("ping")).thenReturn(new float[] {1f});
    EmbeddingHealthIndicator indicator = new EmbeddingHealthIndicator(embeddings);

    indicator.health();
    indicator.health();
    indicator.health();

    verify(embeddings, times(1)).embed("ping");
  }
}
