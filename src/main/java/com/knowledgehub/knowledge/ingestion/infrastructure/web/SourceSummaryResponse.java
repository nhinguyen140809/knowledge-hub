package com.knowledgehub.knowledge.ingestion.infrastructure.web;

import com.knowledgehub.knowledge.ingestion.application.SourceSummary;
import java.time.Instant;

/** JSON view of the source freshness roll-up. */
public record SourceSummaryResponse(
    int total, int synced, int neverSynced, int stale, Instant lastSyncAt) {

  static SourceSummaryResponse from(SourceSummary summary) {
    return new SourceSummaryResponse(
        summary.total(),
        summary.synced(),
        summary.neverSynced(),
        summary.stale(),
        summary.lastSyncAt());
  }
}
