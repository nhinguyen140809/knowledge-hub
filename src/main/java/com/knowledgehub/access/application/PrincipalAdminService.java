package com.knowledgehub.access.application;

import com.knowledgehub.access.domain.AuthenticatedPrincipal;
import com.knowledgehub.access.domain.DefaultPolicy;
import com.knowledgehub.access.domain.PermissionOrigin;
import com.knowledgehub.access.domain.Principal;
import com.knowledgehub.access.domain.PrincipalType;
import com.knowledgehub.access.domain.Role;
import com.knowledgehub.access.domain.exception.AdminGrantException;
import com.knowledgehub.access.domain.exception.AdminMembershipException;
import com.knowledgehub.access.domain.exception.DuplicatePrincipalException;
import com.knowledgehub.access.domain.exception.LastAdminException;
import com.knowledgehub.access.domain.exception.MembershipCycleException;
import com.knowledgehub.access.domain.port.Authorizer;
import com.knowledgehub.access.domain.port.GrantRepository;
import com.knowledgehub.access.domain.port.PrincipalRepository;
import com.knowledgehub.access.domain.port.SystemConfigRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

  @Transactional
  public Principal create(String principalId, PrincipalType type, Role role) {
    if (type == PrincipalType.GROUP && role == Role.ADMIN) {
      throw new IllegalArgumentException("A group cannot have role ADMIN: " + principalId);
    }
    if (principals.findById(principalId).isPresent()) {
      throw new DuplicatePrincipalException(principalId);
    }
    return principals.save(new Principal(principalId, type, role));
  }

  @Transactional(readOnly = true)
  public Principal get(String principalId) {
    return principals
        .findById(principalId)
        .orElseThrow(() -> new PrincipalNotFoundException(principalId));
  }

  @Transactional(readOnly = true)
  public List<Principal> list() {
    return principals.findAll();
  }

  @Transactional
  public void delete(String principalId) {
    Principal principal = get(principalId);
    if (principal.isAdmin() && principals.countByRole(Role.ADMIN) <= 1) {
      throw new LastAdminException(principalId);
    }
    principals.deleteById(principalId);
  }

  // --- group membership ---

  @Transactional
  public void addMember(String groupId, String memberId) {
    requireGroup(groupId);
    Principal member = get(memberId);
    if (member.isAdmin()) {
      throw new AdminMembershipException(memberId);
    }
    if (principals.wouldCreateCycle(groupId, memberId)) {
      throw new MembershipCycleException(groupId, memberId);
    }
    principals.addMember(groupId, memberId);
  }

  @Transactional
  public void removeMember(String groupId, String memberId) {
    requireGroup(groupId);
    principals.removeMember(groupId, memberId);
  }

  @Transactional(readOnly = true)
  public List<String> members(String groupId) {
    requireGroup(groupId);
    return principals.membersOf(groupId);
  }

  /**
   * Atomically moves {@code memberId} from {@code fromGroupId} (null when not yet in a group) to
   * {@code toGroupId}. One transaction, so a failure never leaves the principal in both groups or
   * in neither.
   */
  @Transactional
  public void move(String memberId, String fromGroupId, String toGroupId) {
    Principal member = get(memberId);
    requireGroup(toGroupId);
    if (member.isAdmin()) {
      throw new AdminMembershipException(memberId);
    }
    if (principals.wouldCreateCycle(toGroupId, memberId)) {
      throw new MembershipCycleException(toGroupId, memberId);
    }
    if (fromGroupId != null) {
      requireGroup(fromGroupId);
      principals.removeMember(fromGroupId, memberId);
    }
    principals.addMember(toGroupId, memberId);
  }

  @Transactional(readOnly = true)
  public PrincipalGraph graph() {
    return new PrincipalGraph(principals.findAll(), principals.membershipGraph());
  }

  // --- grants ---

  @Transactional
  public void grant(String principalId, Collection<String> sourceIds) {
    Principal principal = get(principalId);
    if (principal.isAdmin()) {
      throw new AdminGrantException(principalId);
    }
    grants.grant(principalId, sourceIds);
  }

  @Transactional
  public void revokeGrant(String principalId, Collection<String> sourceIds) {
    get(principalId);
    grants.revoke(principalId, sourceIds);
  }

  @Transactional(readOnly = true)
  public List<String> grantedSources(String principalId) {
    get(principalId);
    return grants.grantedSources(principalId);
  }

  // --- default policy ---

  @Transactional(readOnly = true)
  public DefaultPolicy defaultPolicy() {
    return systemConfig.defaultPolicy();
  }

  @Transactional
  public void setDefaultPolicy(DefaultPolicy policy) {
    systemConfig.setDefaultPolicy(policy);
  }

  // --- inspection ---

  @Transactional(readOnly = true)
  public EffectivePermissions effectivePermissions(String principalId) {
    Principal principal = get(principalId);
    AuthenticatedPrincipal authenticated =
        new AuthenticatedPrincipal(principal.principalId(), principal.role());
    Set<String> readable = authorizer.readableSources(authenticated);
    Map<String, Set<String>> grantingPrincipals = grants.grantingPrincipalsFor(principalId);
    List<EffectivePermissions.SourceAccess> sources =
        readable.stream().map(sourceId -> resolveAccess(principal, sourceId, grantingPrincipals)).toList();
    return new EffectivePermissions(principalId, systemConfig.defaultPolicy(), sources);
  }

  /** Picks the one origin that matters most (see {@link PermissionOrigin}) for a readable source. */
  private static EffectivePermissions.SourceAccess resolveAccess(
      Principal principal, String sourceId, Map<String, Set<String>> grantingPrincipals) {
    Set<String> via = grantingPrincipals.getOrDefault(sourceId, Set.of());
    PermissionOrigin origin;
    if (via.contains(principal.principalId())) {
      origin = PermissionOrigin.DIRECT;
    } else if (!via.isEmpty()) {
      origin = PermissionOrigin.INHERITED;
    } else if (principal.isAdmin()) {
      origin = PermissionOrigin.ADMIN;
    } else {
      origin = PermissionOrigin.POLICY;
    }
    return new EffectivePermissions.SourceAccess(sourceId, origin, via);
  }

  private void requireGroup(String groupId) {
    if (get(groupId).type() != PrincipalType.GROUP) {
      throw new IllegalArgumentException("Principal is not a group: " + groupId);
    }
  }
}
