package com.knowledgehub.access.domain;

/**
 * The resolved identity attached to a request after authentication. Carried in the security context
 * for the duration of the request; authorization reads it to decide readable sources and admin
 * rights.
 *
 * @param principalId the authenticated principal's id
 * @param role its role
 */
public record AuthenticatedPrincipal(String principalId, Role role) {

  public boolean isAdmin() {
    return role == Role.ADMIN;
  }
}
