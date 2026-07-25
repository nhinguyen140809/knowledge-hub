package com.knowledgehub.shared.config;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Chunking tunables, bound from {@code app.chunk.*}.
 *
 * @param maxTokens target maximum tokens per chunk (default 512)
 * @param overlap token overlap between adjacent chunks (default 64)
 */
@Validated
@ConfigurationProperties("app.chunk")
public record ChunkProperties(@Positive Integer maxTokens, @PositiveOrZero Integer overlap) {
  public ChunkProperties {
    if (maxTokens == null) {
      maxTokens = 512;
    }
    if (overlap == null) {
      overlap = 64;
    }
  }
}
