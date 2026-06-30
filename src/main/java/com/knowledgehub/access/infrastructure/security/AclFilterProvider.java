package com.knowledgehub.access.infrastructure.security;

import com.knowledgehub.access.domain.AuthenticatedPrincipal;
import com.knowledgehub.access.domain.Authorizer;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Supplies the readable-source set for the current request, read from the security context the
 * authentication filter populated. Retrieval threads this set into every search path as a hard
 * pre-filter. If there is no authenticated principal, nothing is readable (fail-closed).
 */
@Component
public class AclFilterProvider {

  private final Authorizer authorizer;

  public AclFilterProvider(Authorizer authorizer) {
    this.authorizer = authorizer;
  }

  /** The sources the current caller may read. */
  public Set<String> currentAllowedSources() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !(authentication.getPrincipal() instanceof AuthenticatedPrincipal principal)) {
      return Set.of();
    }
    return authorizer.readableSources(principal);
  }
}
