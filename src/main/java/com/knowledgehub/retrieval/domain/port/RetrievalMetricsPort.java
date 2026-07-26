package com.knowledgehub.retrieval.domain.port;

import java.time.Duration;

/**
 * Retrieval activity since process start: how many queries were served and how long they took. How
 * that's recorded and read back is the adapter's business.
 */
public interface RetrievalMetricsPort {

  /** Records one served query and its end-to-end latency. */
  void recordQuery(Duration elapsed);

  long queriesServedCount();

  double p50Millis();

  double p95Millis();
}
