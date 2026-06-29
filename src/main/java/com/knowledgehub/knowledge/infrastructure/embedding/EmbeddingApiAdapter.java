package com.knowledgehub.knowledge.infrastructure.embedding;

import com.knowledgehub.knowledge.domain.EmbeddingPort;
import java.util.List;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

/**
 * {@link EmbeddingPort} backed by a Spring AI {@link EmbeddingModel} (OpenAI-compatible, hosted or
 * self-hosted, selected by config). This is the single outbound boundary to the embedding provider.
 *
 * <p>TODO(P0): wrap calls with Resilience4j {@code @Retry}/{@code @CircuitBreaker} + fallback once
 * the dependency is added — resilience belongs here on the adapter, not on services.
 */
@Component
public class EmbeddingApiAdapter implements EmbeddingPort {

  private final EmbeddingModel embeddingModel;

  public EmbeddingApiAdapter(EmbeddingModel embeddingModel) {
    this.embeddingModel = embeddingModel;
  }

  @Override
  public float[] embed(String text) {
    return embeddingModel.embed(text);
  }

  @Override
  public List<float[]> embedBatch(List<String> texts) {
    return embeddingModel.embed(texts);
  }

  @Override
  public int dimension() {
    return embeddingModel.dimensions();
  }
}
