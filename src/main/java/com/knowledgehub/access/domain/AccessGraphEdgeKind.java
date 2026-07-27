package com.knowledgehub.access.domain;

/** What an access-graph edge represents: group containment, or a read grant to a source. */
public enum AccessGraphEdgeKind {
  /** {@code from} is a group, {@code to} is its direct member (a subject or a nested group). */
  MEMBER,
  /** {@code from} is a principal (self or a group), {@code to} is a source it was granted. */
  GRANT
}
