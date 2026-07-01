package com.knowledgehub.access.application;

import com.knowledgehub.access.domain.AuthenticatedPrincipal;
import com.knowledgehub.access.domain.Authorizer;
import com.knowledgehub.access.domain.DefaultPolicy;
import com.knowledgehub.access.domain.GrantRepository;
import com.knowledgehub.access.domain.Principal;
import com.knowledgehub.access.domain.PrincipalRepository;
import com.knowledgehub.access.domain.PrincipalType;
import com.knowledgehub.access.domain.Role;
import com.knowledgehub.access.domain.SystemConfigRepository;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Administers principals, group membership, grants and the default policy, and resolves a
 * principal's effective permissions. All mutating operations here are admin-only at the API
 * boundary; this service assumes the caller is already authorized.
 */
@Service
public class PrincipalAdminService {

  private final PrincipalRepository principals;
  private final GrantRepository grants;
  private final SystemConfigRepository systemConfig;
  private final Authorizer authorizer;

  public PrincipalAdminService(
      PrincipalRepository principals,
      GrantRepository grants,
      SystemConfigRepository systemConfig,
      Authorizer authorizer) {
    this.principals = principals;
    this.grants = grants;
    this.systemConfig = systemConfig;
    this.authorizer = authorizer;
  }

  // --- principals ---

  public Principal create(String principalId, PrincipalType type, Role role) {
    if (principals.findById(principalId).isPresent()) {
      throw new DuplicatePrincipalException(principalId);
    }
    return principals.save(new Principal(principalId, type, role));
  }

  public Principal get(String principalId) {
    return principals
        .findById(principalId)
        .orElseThrow(() -> new PrincipalNotFoundException(principalId));
  }

  public List<Principal> list() {
    return principals.findAll();
  }

  public void delete(String principalId) {
    get(principalId);
    principals.deleteById(principalId);
  }

  // --- group membership ---

  public void addMember(String groupId, String memberId) {
    requireGroup(groupId);
    get(memberId);
    principals.addMember(groupId, memberId);
  }

  public void removeMember(String groupId, String memberId) {
    requireGroup(groupId);
    principals.removeMember(groupId, memberId);
  }

  public List<String> members(String groupId) {
    requireGroup(groupId);
    return principals.membersOf(groupId);
  }

  // --- grants ---

  public void grant(String principalId, Collection<String> sourceIds) {
    get(principalId);
    grants.grant(principalId, sourceIds);
  }

  public void revokeGrant(String principalId, Collection<String> sourceIds) {
    get(principalId);
    grants.revoke(principalId, sourceIds);
  }

  public List<String> grantedSources(String principalId) {
    get(principalId);
    return grants.grantedSources(principalId);
  }

  // --- default policy ---

  public DefaultPolicy defaultPolicy() {
    return systemConfig.defaultPolicy();
  }

  public void setDefaultPolicy(DefaultPolicy policy) {
    systemConfig.setDefaultPolicy(policy);
  }

  // --- inspection ---

  public EffectivePermissions effectivePermissions(String principalId) {
    Principal principal = get(principalId);
    AuthenticatedPrincipal authenticated =
        new AuthenticatedPrincipal(principal.principalId(), principal.role());
    return new EffectivePermissions(
        principalId,
        systemConfig.defaultPolicy(),
        authorizer.readableSources(authenticated),
        grants.grantingPrincipalsFor(principalId));
  }

  private void requireGroup(String groupId) {
    if (get(groupId).type() != PrincipalType.GROUP) {
      throw new IllegalArgumentException("Principal is not a group: " + groupId);
    }
  }
}
