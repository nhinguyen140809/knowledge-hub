package com.knowledgehub.shared.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Application-level toggles bound from the {@code app.*} configuration namespace. Centralizes
 * config-as-a-feature switches so they are typed and validated instead of read ad-hoc via
 * {@code @Value} (FR-7).
 *
 * <p>{@code @Validated} makes Spring Boot validate these at startup (fail fast). The hybrid
 * contract is: a <em>missing</em> key falls back to a sensible default (compact constructors
 * below); a <em>present but invalid</em> value (e.g. an unknown mode, a negative size) fails the
 * boot. Nested records are cascaded with {@code @Valid}.
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
    @Valid Embedding embedding, @Valid Chunk chunk, @Valid Retrieval retrieval) {

  public AppProperties {
    if (embedding == null) {
      embedding = new Embedding(null, null);
    }
    if (chunk == null) {
      chunk = new Chunk(null, null);
    }
    if (retrieval == null) {
      retrieval = new Retrieval(null);
    }
  }

  /**
   * Embedding provider selection and vector dimension (FR-7.1). Vectors always live in Qdrant
   * (there is no single-store mode); the dimension here sizes the Qdrant collection and must match
   * the embedding model.
   *
   * @param provider {@code api} (default, hosted/OpenAI-compatible) or {@code local} (self-hosted
   *     OpenAI-compatible endpoint).
   * @param dimension embedding vector dimension (default 1536 for {@code text-embedding-3-small}).
   */
  public record Embedding(
      @Pattern(regexp = "api|local") String provider, @Positive Integer dimension) {
    public Embedding {
      if (provider == null || provider.isBlank()) {
        provider = "api";
      }
      if (dimension == null) {
        dimension = 1536;
      }
    }
  }

  /**
   * Chunking tunables (FR-2.1).
   *
   * @param maxTokens target maximum tokens per chunk (default 512)
   * @param overlap token overlap between adjacent chunks (default 64)
   */
  public record Chunk(@Positive Integer maxTokens, @PositiveOrZero Integer overlap) {
    public Chunk {
      if (maxTokens == null) {
        maxTokens = 512;
      }
      if (overlap == null) {
        overlap = 64;
      }
    }
  }

  /**
   * Retrieval tunables (FR-4.4).
   *
   * @param topK default number of results when the caller does not specify (default 10)
   */
  public record Retrieval(@Positive Integer topK) {
    public Retrieval {
      if (topK == null) {
        topK = 10;
      }
    }
  }
}
