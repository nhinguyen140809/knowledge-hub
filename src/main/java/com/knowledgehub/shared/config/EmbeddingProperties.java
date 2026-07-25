package com.knowledgehub.shared.config;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Embedding provider selection and vector dimension, bound from {@code app.embedding.*}. Vectors
 * always live in Qdrant (there is no single-store mode); the dimension sizes the Qdrant collection
 * and must match the embedding model. A missing key falls back to the default in the compact
 * constructor; a present-but-invalid value fails the boot.
 *
 * @param provider {@code api} (default, hosted/OpenAI-compatible) or {@code local} (self-hosted
 *     OpenAI-compatible endpoint).
 * @param dimension embedding vector dimension (default 1536 for {@code text-embedding-3-small}).
 * @param retryMaxAttempts how many times an embedding call is attempted before failing (default 3,
 *     including the first try).
 * @param retryBackoffMs initial backoff between embedding retries in milliseconds; doubles each
 *     attempt (default 500).
 */
@Validated
@ConfigurationProperties("app.embedding")
public record EmbeddingProperties(
    @Pattern(regexp = "api|local") String provider,
    @Positive Integer dimension,
    @Positive Integer retryMaxAttempts,
    @Positive Integer retryBackoffMs) {
  public EmbeddingProperties {
    if (provider == null || provider.isBlank()) {
      provider = "api";
    }
    if (dimension == null) {
      dimension = 1536;
    }
    if (retryMaxAttempts == null) {
      retryMaxAttempts = 3;
    }
    if (retryBackoffMs == null) {
      retryBackoffMs = 500;
    }
  }
}
