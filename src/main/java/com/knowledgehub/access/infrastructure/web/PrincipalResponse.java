package com.knowledgehub.access.infrastructure.web;

import com.knowledgehub.access.domain.Principal;
import com.knowledgehub.access.domain.PrincipalType;
import com.knowledgehub.access.domain.Role;

/** JSON view of a principal. */
public record PrincipalResponse(String principalId, PrincipalType type, Role role) {

  static PrincipalResponse from(Principal principal) {
    return new PrincipalResponse(principal.principalId(), principal.type(), principal.role());
  }
}
