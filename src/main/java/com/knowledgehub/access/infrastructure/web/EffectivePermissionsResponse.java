package com.knowledgehub.access.infrastructure.web;

import com.knowledgehub.access.application.EffectivePermissions;
import com.knowledgehub.access.domain.DefaultPolicy;
import java.util.Map;
import java.util.Set;

/**
 * JSON view of a principal's resolved read access. {@code readableSources} is exactly what the
 * retrieval pre-filter applies; {@code grantedVia} explains which principals grant each granted
 * source.
 */
public record EffectivePermissionsResponse(
    String principalId,
    DefaultPolicy defaultPolicy,
    Set<String> readableSources,
    Map<String, Set<String>> grantedVia) {

  static EffectivePermissionsResponse from(EffectivePermissions permissions) {
    return new EffectivePermissionsResponse(
        permissions.principalId(),
        permissions.defaultPolicy(),
        permissions.readableSources(),
        permissions.grantedVia());
  }
}
