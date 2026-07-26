package com.knowledgehub.knowledge.ingestion.application;

import java.time.Instant;

/**
 * At-a-glance freshness roll-up over every configured source, so the dashboard doesn't fetch every
 * source and reduce client-side.
 *
 * @param total configured sources
 * @param synced sources with at least one successful sync
 * @param neverSynced sources never synced
 * @param stale synced sources whose last sync predates the configured staleness threshold
 * @param lastSyncAt the most recent sync instant across all sources, or {@code null} if none has
 *     ever synced
 */
public record SourceSummary(int total, int synced, int neverSynced, int stale, Instant lastSyncAt) {}
