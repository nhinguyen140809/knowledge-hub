package com.knowledgehub.knowledge.domain;

import java.util.Set;

/**
 * Query filter applied at the storage layer (a hard pre-filter, never a post-filter). Present on
 * every search port from day one so retrieval (P4) and ACL (P5) need not change signatures later.
 *
 * @param allowedSources the source ids the caller may read; {@code null} means
 *     <em>unrestricted</em> (no ACL applied yet), an empty set means <em>nothing readable</em>
 * @param ref optional version/branch filter, or {@code null} for none
 * @param type optional data-type filter (code|doc|requirement|commit), or {@code null}
 */
public record Filter(Set<String> allowedSources, String ref, String type) {

  public Filter {
    if (allowedSources != null) {
      allowedSources = Set.copyOf(allowedSources);
    }
  }

  /** A filter with no restriction at all — used before ACL is wired in. */
  public static Filter unrestricted() {
    return new Filter(null, null, null);
  }

  /** Restrict to exactly the given readable sources. */
  public static Filter ofSources(Set<String> allowedSources) {
    return new Filter(allowedSources, null, null);
  }

  /** True when no ACL restriction applies (every source is readable). */
  public boolean isUnrestricted() {
    return allowedSources == null;
  }
}
