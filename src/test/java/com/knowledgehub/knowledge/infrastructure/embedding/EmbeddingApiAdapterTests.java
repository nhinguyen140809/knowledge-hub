package com.knowledgehub.knowledge.infrastructure.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;

class EmbeddingApiAdapterTests {

  @Test
  void delegatesToTheSpringAiEmbeddingModel() {
    EmbeddingModel model = mock(EmbeddingModel.class);
    when(model.embed("hi")).thenReturn(new float[] {1f, 2f});
    when(model.embed(List.of("a", "b"))).thenReturn(List.of(new float[] {1f}, new float[] {2f}));
    when(model.dimensions()).thenReturn(1536);

    EmbeddingApiAdapter adapter = new EmbeddingApiAdapter(model, 1000);

    assertThat(adapter.embed("hi")).containsExactly(1f, 2f);
    assertThat(adapter.embedBatch(List.of("a", "b"))).hasSize(2);
    assertThat(adapter.dimension()).isEqualTo(1536);
  }

  @Test
  void splitsIntoProviderSizedSubBatchesAndPreservesOrder() {
    EmbeddingModel model = mock(EmbeddingModel.class);
    when(model.embed(List.of("a", "b"))).thenReturn(List.of(new float[] {1f}, new float[] {2f}));
    when(model.embed(List.of("c"))).thenReturn(List.of(new float[] {3f}));

    EmbeddingApiAdapter adapter = new EmbeddingApiAdapter(model, 2);

    List<float[]> result = adapter.embedBatch(List.of("a", "b", "c"));

    assertThat(result).hasSize(3);
    assertThat(result.get(0)).containsExactly(1f);
    assertThat(result.get(1)).containsExactly(2f);
    assertThat(result.get(2)).containsExactly(3f);
    verify(model).embed(List.of("a", "b"));
    verify(model).embed(List.of("c"));
  }
}
