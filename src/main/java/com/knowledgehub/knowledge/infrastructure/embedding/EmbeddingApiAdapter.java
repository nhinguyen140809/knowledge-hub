package com.knowledgehub.knowledge.infrastructure.embedding;

import com.knowledgehub.knowledge.domain.EmbeddingPort;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

/**
 * {@link EmbeddingPort} backed by a Spring AI {@link EmbeddingModel} (OpenAI-compatible, hosted or
 * self-hosted, selected by config). This is the single outbound boundary to the embedding provider,
 * so resilience lives here: each call is retried with exponential backoff before failing, and the
 * exhaustion is logged. Defaults can be tuned via {@code app.embedding.retry-*} config.
 */
@Component
public class EmbeddingApiAdapter implements EmbeddingPort {

  private static final Logger log = LoggerFactory.getLogger(EmbeddingApiAdapter.class);

  private final EmbeddingModel embeddingModel;

  public EmbeddingApiAdapter(EmbeddingModel embeddingModel) {
    this.embeddingModel = embeddingModel;
  }

  @Override
  @Retryable(
      retryFor = Exception.class,
      maxAttemptsExpression = "${app.embedding.retry-max-attempts:3}",
      backoff =
          @Backoff(delayExpression = "${app.embedding.retry-backoff-ms:500}", multiplier = 2.0))
  public float[] embed(String text) {
    return embeddingModel.embed(text);
  }

  @Override
  @Retryable(
      retryFor = Exception.class,
      maxAttemptsExpression = "${app.embedding.retry-max-attempts:3}",
      backoff =
          @Backoff(delayExpression = "${app.embedding.retry-backoff-ms:500}", multiplier = 2.0))
  public List<float[]> embedBatch(List<String> texts) {
    return embeddingModel.embed(texts);
  }

  @Override
  public int dimension() {
    return embeddingModel.dimensions();
  }

  /** Invoked when {@link #embed} has exhausted its retries — log and surface a clear failure. */
  @Recover
  float[] recoverEmbed(Exception cause, String text) {
    log.error("Embedding failed after retries for a single text: {}", cause.toString());
    throw new IllegalStateException("Embedding provider failed after retries", cause);
  }

  /**
   * Invoked when {@link #embedBatch} has exhausted its retries — log and surface a clear failure.
   */
  @Recover
  List<float[]> recoverEmbedBatch(Exception cause, List<String> texts) {
    log.error("Embedding failed after retries for {} texts: {}", texts.size(), cause.toString());
    throw new IllegalStateException("Embedding provider failed after retries", cause);
  }
}
