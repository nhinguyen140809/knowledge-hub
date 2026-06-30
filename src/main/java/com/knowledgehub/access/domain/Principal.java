package com.knowledgehub.access.domain;

import java.util.Objects;

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
}
