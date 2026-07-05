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
 * @param commitsIndexed commits newly added to the knowledge graph (0 for a non-git source)
 * @param durationMs wall-clock duration of the sync
 * @param toCommit the commit the source is at now, or {@code null} for a non-git source
 * @param idempotent true when the sync changed nothing (no file changed and no commit was new)
 */
public record SyncResult(
    String sourceId,
    int indexed,
    int reindexed,
    int evicted,
    int skipped,
    int commitsIndexed,
    long durationMs,
    String toCommit,
    boolean idempotent) {

  /**
   * The result of a sync whose files were all unchanged. New commits can still have been indexed —
   * a commit does not have to touch an indexed file — so the run only counts as idempotent when
   * there were none.
   */
  public static SyncResult noChange(
      String sourceId, int skipped, int commitsIndexed, long durationMs, String toCommit) {
    return new SyncResult(
        sourceId, 0, 0, 0, skipped, commitsIndexed, durationMs, toCommit, commitsIndexed == 0);
  }
}
