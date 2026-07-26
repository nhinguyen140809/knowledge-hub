package com.knowledgehub.access.domain;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A subject or group that can be granted access. The unit that authentication resolves to and that
 * authorization reasons about. Membership (a subject belonging to a group) and credentials are
 * modelled as relationships, not fields, so this carries only identity, kind and role.
 *
 * @param principalId stable unique id
 * @param type subject or group
 * @param role whether the principal may administer the system
 */
public record Principal(String principalId, PrincipalType type, Role role) {

  public Principal {
    if (principalId == null || principalId.isBlank()) {
      throw new IllegalArgumentException("principalId must not be blank");
    }
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(role, "role");
  }

  public boolean isAdmin() {
    return role == Role.ADMIN;
  }

  /**
   * The grant-based reason this principal can read a source, given the grantors whose access
   * reaches it (empty when none do). Empty when no grant path reaches the source at all — that
   * case carries no origin of its own, so the caller decides what it means (a default-allow
   * policy fallback, or not readable).
   */
  public Optional<PermissionOrigin> originFor(Set<String> via) {
    if (via.contains(principalId)) {
      return Optional.of(PermissionOrigin.DIRECT);
    }
    if (!via.isEmpty()) {
      return Optional.of(PermissionOrigin.INHERITED);
    }
    if (isAdmin()) {
      return Optional.of(PermissionOrigin.ADMIN);
    }
    return Optional.empty();
  }
}
