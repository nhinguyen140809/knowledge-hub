package com.knowledgehub.knowledge.sync.domain;

/**
 * Summary of one sync run. {@code idempotent} is true when nothing changed, so a caller can tell a
 * real update from a no-op re-trigger.
 *
 * @param sourceId the source synced
 * @param indexed files newly indexed
 * @param reindexed files re-indexed after a change
 * @param evicted files removed from the index
 * @param skipped files left untouched because they were unchanged
 * @param durationMs wall-clock duration of the sync
 * @param toCommit the commit the source is at now, or {@code null} for a non-git source
 * @param idempotent true when the source had no changes (the sync was a no-op)
 */
public record SyncResult(
    String sourceId,
    int indexed,
    int reindexed,
    int evicted,
    int skipped,
    long durationMs,
    String toCommit,
    boolean idempotent) {

  /** A no-op result for a source that had no changes. */
  public static SyncResult noChange(
      String sourceId, int skipped, long durationMs, String toCommit) {
    return new SyncResult(sourceId, 0, 0, 0, skipped, durationMs, toCommit, true);
  }
}
