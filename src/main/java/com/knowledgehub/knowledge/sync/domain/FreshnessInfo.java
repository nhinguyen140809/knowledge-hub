package com.knowledgehub.knowledge.sync.domain;

import java.time.Instant;

/**
 * How fresh a source's index is: when it was last indexed and, for a git source, at which commit
 * and ref. Reported on the status endpoint and used by the differ to find the starting point of the
 * next sync.
 *
 * @param sourceId the source
 * @param indexedAt when the source was last fully synced
 * @param commitSha the commit last indexed, or {@code null} for a non-git source
 * @param ref the ref last indexed, or {@code null}
 */
public record FreshnessInfo(String sourceId, Instant indexedAt, String commitSha, String ref) {}
