package com.knowledgehub.system.infrastructure.health;

import com.knowledgehub.knowledge.domain.port.EmbeddingPort;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Reachability of the embedding provider. There is no cheap "ping" for an embedding API, so this
 * embeds a short placeholder string through the same {@link EmbeddingPort} indexing uses (retries
 * included) and caches the result for {@link #CACHE_TTL} — probing on every health poll would cost
 * real API quota and could itself trip the provider's rate limit. Registered under the name {@code
 * embeddings} (the explicit bean name), read back by {@code DependencyHealthService}.
 */
@Component("embeddings")
public class EmbeddingHealthIndicator implements HealthIndicator {

  private static final Logger log = LoggerFactory.getLogger(EmbeddingHealthIndicator.class);
  private static final Duration CACHE_TTL = Duration.ofMinutes(15);
  private static final String PING_TEXT = "ping";

  private final EmbeddingPort embeddings;

  private Health cached;
  private Instant cachedAt = Instant.EPOCH;

  public EmbeddingHealthIndicator(EmbeddingPort embeddings) {
    this.embeddings = embeddings;
  }

  @Override
  public synchronized Health health() {
    if (cached == null || Instant.now().isAfter(cachedAt.plus(CACHE_TTL))) {
      cached = probe();
      cachedAt = Instant.now();
    }
    return cached;
  }

  private Health probe() {
    try {
      embeddings.embed(PING_TEXT);
      return Health.up().build();
    } catch (Exception e) {
      log.warn("Embedding provider health probe failed: {}", e.toString());
      return Health.down(e).build();
    }
  }
}
