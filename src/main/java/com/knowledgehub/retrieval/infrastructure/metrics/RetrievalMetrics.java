package com.knowledgehub.retrieval.infrastructure.metrics;

import com.knowledgehub.retrieval.domain.port.RetrievalMetricsPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.ValueAtPercentile;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/**
 * Micrometer-backed {@link RetrievalMetricsPort}. Micrometer timers don't publish percentiles by
 * default, so this configures p50/p95 explicitly.
 */
@Component
public class RetrievalMetrics implements RetrievalMetricsPort {

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

  @Override
  public void recordQuery(Duration elapsed) {
    queriesServed.increment();
    queryLatency.record(elapsed);
  }

  @Override
  public long queriesServedCount() {
    return (long) queriesServed.count();
  }

  @Override
  public double p50Millis() {
    return percentile(0.5);
  }

  @Override
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
