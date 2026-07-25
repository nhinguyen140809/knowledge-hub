package com.knowledgehub.shared.config;

import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Access-control tunables, bound from {@code app.security.*}.
 *
 * @param apiKey the bootstrap admin secret. On startup, if no admin principal exists yet, an admin
 *     is created and the hash of this key is registered as its credential. Blank disables bootstrap
 *     (no admin is seeded). Never logged.
 * @param credentialRetentionMonths how long a revoked credential is kept before the retention job
 *     purges it; the node is held until then so audit trails stay intact (default 12)
 * @param aclCacheTtl how long a principal's resolved readable-source set may be cached; kept short
 *     so a grant/policy change takes effect promptly without a restart (default 5s)
 */
@Validated
@ConfigurationProperties("app.security")
public record SecurityProperties(
    String apiKey, @Positive Integer credentialRetentionMonths, Duration aclCacheTtl) {
  public SecurityProperties {
    if (credentialRetentionMonths == null) {
      credentialRetentionMonths = 12;
    }
    if (aclCacheTtl == null) {
      aclCacheTtl = Duration.ofSeconds(5);
    }
  }
}
