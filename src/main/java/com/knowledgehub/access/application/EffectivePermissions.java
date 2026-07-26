package com.knowledgehub.access.application;

import com.knowledgehub.access.domain.DefaultPolicy;
import com.knowledgehub.access.domain.PermissionOrigin;
import java.util.List;
import java.util.Set;

/**
 * A principal's resolved read access, for inspection and debugging. {@code sources} is exactly
 * the set the retrieval pre-filter uses, each annotated with why it's readable.
 *
 * @param principalId the principal inspected
 * @param defaultPolicy the policy in force
 * @param sources the sources the principal may read, each with its access origin
 */
public record EffectivePermissions(
    String principalId, DefaultPolicy defaultPolicy, List<SourceAccess> sources) {

  /**
   * @param sourceId a source the principal may read
   * @param origin why it's readable, see {@link PermissionOrigin}
   * @param via every principal (the principal itself or a group it belongs to) whose grant
   *     reaches this source; empty for the ADMIN and POLICY origins, which have no grant path
   */
  public record SourceAccess(String sourceId, PermissionOrigin origin, Set<String> via) {}
}
