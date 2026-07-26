package com.knowledgehub.system.domain;

/**
 * Retrieval activity since process start — not persisted, so a restart resets these to zero. The
 * caller labels them accordingly rather than implying an all-time total.
 *
 * @param queriesServed total queries served since process start
 * @param p50LatencyMs median end-to-end query latency
 * @param p95LatencyMs 95th-percentile end-to-end query latency
 * @param cacheHitRate share of queries answered from the retrieval cache, 0..1
 */
public record RetrievalStats(
    long queriesServed, double p50LatencyMs, double p95LatencyMs, double cacheHitRate) {}
