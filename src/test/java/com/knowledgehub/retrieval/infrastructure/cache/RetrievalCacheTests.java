package com.knowledgehub.retrieval.infrastructure.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledgehub.retrieval.domain.Query;
import com.knowledgehub.retrieval.domain.RankedResult;
import com.knowledgehub.shared.config.AppProperties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class RetrievalCacheTests {

  private final RetrievalCache cache =
      new RetrievalCache(new AppProperties(null, null, null, null, null));

  @Test
  void computesOncePerQueryThenServesFromCache() {
    AtomicInteger computations = new AtomicInteger();
    Supplier<RankedResult> compute =
        () -> {
          computations.incrementAndGet();
          return RankedResult.empty();
        };
    Query query = Query.of("hello world");

    cache.get(query, null, compute);
    cache.get(query, null, compute);

    assertThat(computations.get()).isEqualTo(1);
  }

  @Test
  void doesNotShareAcrossDifferentAllowedSources() {
    AtomicInteger computations = new AtomicInteger();
    Supplier<RankedResult> compute =
        () -> {
          computations.incrementAndGet();
          return RankedResult.empty();
        };
    Query query = Query.of("hello world");

    cache.get(query, Set.of("a"), compute);
    cache.get(query, Set.of("b"), compute);

    // A different allow-list is a different key, so one principal never gets another's answer.
    assertThat(computations.get()).isEqualTo(2);
  }
}
