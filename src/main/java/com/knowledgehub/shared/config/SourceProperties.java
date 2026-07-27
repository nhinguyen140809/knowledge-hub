package com.knowledgehub.shared.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Source freshness tunables, bound from {@code app.sources.*}.
 *
 * @param staleAfter how long since its last sync before a source counts as stale in the summary
 *     (default 7 days)
 */
@Validated
@ConfigurationProperties("app.sources")
public record SourceProperties(Duration staleAfter) {
  public SourceProperties {
    if (staleAfter == null) {
      staleAfter = Duration.ofDays(7);
    }
  }
}
