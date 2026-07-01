package com.knowledgehub.access.domain;

import java.util.Set;

/** Decides what an authenticated principal may read and whether it may administer the system. */
public interface Authorizer {

  /**
   * The full set of source ids the principal may read, after unioning its grants with those of
   * every group it belongs to and applying the default policy.
   */
  Set<String> readableSources(AuthenticatedPrincipal principal);

  boolean isAdmin(AuthenticatedPrincipal principal);
}
