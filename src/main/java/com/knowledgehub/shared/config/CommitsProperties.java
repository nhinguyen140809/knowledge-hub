package com.knowledgehub.shared.config;

import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Commit-history indexing tunables, bound from {@code app.commits.*}.
 *
 * @param historyDepth how many of the newest commits are indexed per source (default 100). Each
 *     sync only processes commits not indexed yet, so the depth bounds the first walk of a source
 *     and the recovery after a history rewrite, not the steady state. Zero disables commit indexing
 *     entirely.
 */
@Validated
@ConfigurationProperties("app.commits")
public record CommitsProperties(@PositiveOrZero Integer historyDepth) {
  public CommitsProperties {
    if (historyDepth == null) {
      historyDepth = 100;
    }
  }
}
