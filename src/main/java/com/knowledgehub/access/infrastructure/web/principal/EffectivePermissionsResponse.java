package com.knowledgehub.access.infrastructure.web.principal;

import com.knowledgehub.access.application.EffectivePermissions;
import com.knowledgehub.access.domain.DefaultPolicy;
import com.knowledgehub.access.domain.PermissionOrigin;
import java.util.List;
import java.util.Set;

/**
 * JSON view of a principal's resolved read access. {@code sources} is exactly what the retrieval
 * pre-filter applies, each entry explaining why it's readable.
 */
public record EffectivePermissionsResponse(
    String principalId, DefaultPolicy defaultPolicy, List<SourceAccessResponse> sources) {

  static EffectivePermissionsResponse from(EffectivePermissions permissions) {
    return new EffectivePermissionsResponse(
        permissions.principalId(),
        permissions.defaultPolicy(),
        permissions.sources().stream().map(SourceAccessResponse::from).toList());
  }

  public record SourceAccessResponse(String sourceId, PermissionOrigin origin, Set<String> via) {
    static SourceAccessResponse from(EffectivePermissions.SourceAccess access) {
      return new SourceAccessResponse(access.sourceId(), access.origin(), access.via());
    }
  }
}
