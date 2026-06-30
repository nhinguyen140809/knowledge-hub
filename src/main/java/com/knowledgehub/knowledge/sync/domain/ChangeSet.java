package com.knowledgehub.knowledge.sync.domain;

import java.util.List;
import java.util.stream.Stream;

/**
 * The files that changed in a source since it was last indexed, split by how they changed.
 * Unchanged files are deliberately absent - sync does nothing for them - so an empty change set
 * (nothing added, modified or deleted) means a re-sync is a no-op.
 *
 * @param sourceId the source the changes belong to
 * @param added paths present now but not at the last index
 * @param modified paths whose content hash changed
 * @param deleted paths indexed before but gone now
 * @param toCommit the commit the source is at now, or {@code null} for a non-git source
 */
public record ChangeSet(
    String sourceId,
    List<String> added,
    List<String> modified,
    List<String> deleted,
    String toCommit) {

  public ChangeSet {
    added = List.copyOf(added);
    modified = List.copyOf(modified);
    deleted = List.copyOf(deleted);
  }

  /** No file was added, modified or deleted - a re-sync would change nothing. */
  public boolean isEmpty() {
    return added.isEmpty() && modified.isEmpty() && deleted.isEmpty();
  }

  /** The paths to (re-)index: everything added or modified. */
  public List<String> toIndex() {
    return Stream.concat(added.stream(), modified.stream()).toList();
  }
}
