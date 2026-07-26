package com.knowledgehub.access.application;

import com.knowledgehub.access.domain.PermissionOrigin;
import java.util.List;
import java.util.Set;

/**
 * Every principal that can read one source — the inverse of {@link EffectivePermissions}.
 *
 * @param sourceId the source inspected
 * @param principals the principals that can read it, each with its access origin
 */
public record SourcePrincipals(String sourceId, List<PrincipalAccess> principals) {

  /**
   * @param principalId a principal that can read the source
   * @param origin why it's readable, see {@link PermissionOrigin}
   * @param via every principal (itself, for DIRECT, or the group(s) it inherits through, for
   *     INHERITED) whose grant reaches it; empty for the ADMIN and POLICY origins
   */
  public record PrincipalAccess(String principalId, PermissionOrigin origin, Set<String> via) {}
}
