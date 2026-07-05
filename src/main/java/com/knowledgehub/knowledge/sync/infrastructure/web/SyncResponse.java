package com.knowledgehub.knowledge.sync.infrastructure.web;

import com.knowledgehub.knowledge.sync.domain.SyncResult;

/**
 * JSON response for a sync run: what the sync did, plus {@code idempotent} so a caller can tell a
 * real update from a no-op re-trigger.
 *
 * @param sourceId the source synced
 * @param indexed files newly indexed
 * @param reindexed files re-indexed after a change
 * @param evicted files removed from the index
 * @param skipped files left untouched
 * @param commitsIndexed commits newly added to the knowledge graph (0 for a non-git source)
 * @param durationMs duration of the sync
 * @param toCommit the commit synced to, or {@code null} for a non-git source
 * @param idempotent true when nothing changed (the sync was a no-op)
 */
public record SyncResponse(
    String sourceId,
    int indexed,
    int reindexed,
    int evicted,
    int skipped,
    int commitsIndexed,
    long durationMs,
    String toCommit,
    boolean idempotent) {

  static SyncResponse from(SyncResult result) {
    return new SyncResponse(
        result.sourceId(),
        result.indexed(),
        result.reindexed(),
        result.evicted(),
        result.skipped(),
        result.commitsIndexed(),
        result.durationMs(),
        result.toCommit(),
        result.idempotent());
  }
}
