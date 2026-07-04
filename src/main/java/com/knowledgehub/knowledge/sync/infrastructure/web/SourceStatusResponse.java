package com.knowledgehub.knowledge.sync.infrastructure.web;

import com.knowledgehub.knowledge.sync.domain.FreshnessInfo;
import java.util.Optional;

/**
 * JSON freshness status for a source, so an agent can decide whether to sync before querying.
 * {@code indexed} is false for a source that has never been synced (the timestamps are then null).
 *
 * @param sourceId the source
 * @param indexed whether the source has been synced at least once
 * @param indexedAt when it was last synced (ISO-8601), or {@code null}
 * @param commitSha the commit last indexed, or {@code null}
 * @param ref the ref last indexed, or {@code null}
 */
public record SourceStatusResponse(
    String sourceId, boolean indexed, String indexedAt, String commitSha, String ref) {

  public static SourceStatusResponse from(String sourceId, Optional<FreshnessInfo> freshness) {
    return freshness
        .map(
            info ->
                new SourceStatusResponse(
                    sourceId, true, info.indexedAt().toString(), info.commitSha(), info.ref()))
        .orElseGet(() -> new SourceStatusResponse(sourceId, false, null, null, null));
  }
}
