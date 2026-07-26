package com.knowledgehub.system.application;

import com.knowledgehub.retrieval.domain.port.ResultCachePort;
import com.knowledgehub.retrieval.domain.port.RetrievalMetricsPort;
import com.knowledgehub.system.domain.RetrievalStats;
import org.springframework.stereotype.Service;

/** Reads back retrieval's own query counters/latency and cache hit rate. */
@Service
public class RetrievalStatsService {

  private final RetrievalMetricsPort metrics;
  private final ResultCachePort cache;

  public RetrievalStatsService(RetrievalMetricsPort metrics, ResultCachePort cache) {
    this.metrics = metrics;
    this.cache = cache;
  }

  public RetrievalStats currentStats() {
    return new RetrievalStats(
        metrics.queriesServedCount(), metrics.p50Millis(), metrics.p95Millis(), cache.hitRate());
  }
}
