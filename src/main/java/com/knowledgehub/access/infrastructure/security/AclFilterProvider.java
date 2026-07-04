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

  /**
   * Resolves the sources the current request's caller may read.
   *
   * <p>The input is implicit: the {@link AuthenticatedPrincipal} the authentication filter placed
   * in the security context of this request's thread. The output is the caller's effective read set
   * — its own grants unioned with the grants of every group it belongs to, interpreted under the
   * default policy.
   *
   * <p>Example: principal {@code alice} holds a grant on {@code docs-service} and belongs to group
   * {@code backend}, which is granted {@code shared-lib}; under a deny-by-default policy this
   * returns {@code ["docs-service", "shared-lib"]}, and every search path drops results from any
   * other source before they leave the store.
   *
   * @return ids of the readable sources; empty when there is no authenticated principal, so an
   *     anonymous caller can read nothing rather than everything
   */
  public Set<String> currentAllowedSources() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !(authentication.getPrincipal() instanceof AuthenticatedPrincipal principal)) {
      return Set.of();
    }
    return authorizer.readableSources(principal);
  }
}
