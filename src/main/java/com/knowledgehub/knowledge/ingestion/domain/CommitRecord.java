package com.knowledgehub.knowledge.ingestion.domain;

import com.knowledgehub.shared.id.IdFactory;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * One commit read from a source's version history: its identity, message, authorship, and the paths
 * its diff against the first parent touched. This is the raw unit of history ingestion, the
 * commit-side sibling of {@link RawArtifact} — indexing turns it into a graph node whose changed
 * paths become edges to the files it modified.
 *
 * <p>For a merge commit the changed paths are the diff against the <em>first</em> parent, so they
 * cover everything the merge brought onto the walked branch.
 *
 * @param sha the full commit hash
 * @param message the full commit message (may be blank, never {@code null})
 * @param author the author as {@code Name <email>}
 * @param authoredAt when the commit was authored
 * @param changedPaths repo-relative paths the commit added, modified, or deleted
 */
public record CommitRecord(
    String sha, String message, String author, Instant authoredAt, List<String> changedPaths) {

  public CommitRecord {
    Objects.requireNonNull(sha, "sha");
    Objects.requireNonNull(message, "message");
    Objects.requireNonNull(author, "author");
    Objects.requireNonNull(authoredAt, "authoredAt");
    changedPaths = changedPaths == null ? List.of() : List.copyOf(changedPaths);
  }

  /**
   * Derives the stable id of the commit with these coordinates, scoped by source so two sources
   * tracking the same repository never share a node (each source's knowledge, including its ACL
   * boundary, stays its own). Mirrors what {@code IdFactory.commitId} used to do.
   */
  public static String deriveId(String sourceId, String sha) {
    return IdFactory.stableId(sourceId, sha);
  }
}
