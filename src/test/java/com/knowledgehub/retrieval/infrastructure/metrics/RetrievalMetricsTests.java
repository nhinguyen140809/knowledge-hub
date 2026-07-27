package com.knowledgehub.retrieval.infrastructure.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class RetrievalMetricsTests {

  @Test
  void reportsZeroBeforeAnyQuery() {
    RetrievalMetrics metrics = new RetrievalMetrics(new SimpleMeterRegistry());

    assertThat(metrics.queriesServedCount()).isZero();
  }

  @Test
  void recordsCountAndLatencyPercentiles() {
    RetrievalMetrics metrics = new RetrievalMetrics(new SimpleMeterRegistry());

    metrics.recordQuery(Duration.ofMillis(10));
    metrics.recordQuery(Duration.ofMillis(20));
    metrics.recordQuery(Duration.ofMillis(30));

    assertThat(metrics.queriesServedCount()).isEqualTo(3);
    assertThat(metrics.p50Millis()).isGreaterThan(0);
    assertThat(metrics.p95Millis()).isGreaterThan(0);
  }
}
