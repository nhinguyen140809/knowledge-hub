package com.knowledgehub.access.infrastructure.web.source;

import com.knowledgehub.access.application.SourcePrincipals;
import com.knowledgehub.access.domain.PermissionOrigin;
import java.util.List;
import java.util.Set;

/** JSON view of every principal that can read a source, the inverse of effective-permissions. */
public record SourcePrincipalsResponse(String sourceId, List<PrincipalAccessResponse> principals) {

  static SourcePrincipalsResponse from(SourcePrincipals resolved) {
    return new SourcePrincipalsResponse(
        resolved.sourceId(),
        resolved.principals().stream().map(PrincipalAccessResponse::from).toList());
  }

  public record PrincipalAccessResponse(
      String principalId, PermissionOrigin origin, Set<String> via) {
    static PrincipalAccessResponse from(SourcePrincipals.PrincipalAccess access) {
      return new PrincipalAccessResponse(access.principalId(), access.origin(), access.via());
    }
  }
}
