package com.knowledgehub.shared.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Knowledge-linking tunables, bound from {@code app.linking.*}.
 *
 * @param confidenceThreshold lowest confidence a cross-artifact link must reach to be written;
 *     candidates below it are dropped (default 0.5). Structural relations are deterministic and
 *     unaffected.
 */
@Validated
@ConfigurationProperties("app.linking")
public record LinkingProperties(@DecimalMin("0.0") @DecimalMax("1.0") Double confidenceThreshold) {
  public LinkingProperties {
    if (confidenceThreshold == null) {
      confidenceThreshold = 0.5;
    }
  }
}
