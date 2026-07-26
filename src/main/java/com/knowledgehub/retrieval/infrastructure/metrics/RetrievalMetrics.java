package com.knowledgehub.retrieval.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.ValueAtPercentile;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/**
 * Retrieval activity since process start: how many queries were served and how long they took.
 * Micrometer timers don't publish percentiles by default, so this configures p50/p95 explicitly —
 * the ones the retrieval-stats dashboard signal reports.
 */
@Component
public class RetrievalMetrics {

  private final Counter queriesServed;
  private final Timer queryLatency;

  public RetrievalMetrics(MeterRegistry registry) {
    this.queriesServed =
        Counter.builder("retrieval.queries.served")
            .description("Queries served since process start")
            .register(registry);
    this.queryLatency =
        Timer.builder("retrieval.query.latency")
            .description("Retrieval query latency")
            .publishPercentiles(0.5, 0.95)
            .register(registry);
  }

  /** Records one served query and its end-to-end latency. */
  public void recordQuery(Duration elapsed) {
    queriesServed.increment();
    queryLatency.record(elapsed);
  }

  public long queriesServedCount() {
    return (long) queriesServed.count();
  }

  public double p50Millis() {
    return percentile(0.5);
  }

  public double p95Millis() {
    return percentile(0.95);
  }

  private double percentile(double target) {
    for (ValueAtPercentile value : queryLatency.takeSnapshot().percentileValues()) {
      if (Double.compare(value.percentile(), target) == 0) {
        return value.value(TimeUnit.MILLISECONDS);
      }
    }
    return 0;
  }
}
