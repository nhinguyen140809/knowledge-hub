package com.knowledgehub.access.domain;

/**
 * Why a source is readable by a principal. When several reasons apply, this is the precedence
 * order (highest first): a direct grant beats an inherited one, which beats the admin-role
 * bypass, which beats the default-allow policy. This is "what can the admin do about it here" — a
 * direct grant is the only one revocable from the principal's panel.
 */
public enum PermissionOrigin {
  /** The principal itself has a grant to the source. */
  DIRECT,
  /** A group the principal belongs to (directly or transitively) has a grant to the source. */
  INHERITED,
  /** The principal's role bypasses grants entirely; no grant path reaches the source. */
  ADMIN,
  /** Readable only because the default policy is ALLOW; no grant path reaches the source. */
  POLICY
}
