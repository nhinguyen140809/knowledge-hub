package com.knowledgehub.system.application;

import com.knowledgehub.retrieval.infrastructure.cache.RetrievalCache;
import com.knowledgehub.retrieval.infrastructure.metrics.RetrievalMetrics;
import com.knowledgehub.system.domain.RetrievalStats;
import org.springframework.stereotype.Service;

/** Reads back retrieval's own Micrometer counters/timer and cache stats for the dashboard. */
@Service
public class RetrievalStatsService {

  private final RetrievalMetrics metrics;
  private final RetrievalCache cache;

  public RetrievalStatsService(RetrievalMetrics metrics, RetrievalCache cache) {
    this.metrics = metrics;
    this.cache = cache;
  }

  public RetrievalStats currentStats() {
    return new RetrievalStats(
        metrics.queriesServedCount(),
        metrics.p50Millis(),
        metrics.p95Millis(),
        cache.stats().hitRate());
  }
}
