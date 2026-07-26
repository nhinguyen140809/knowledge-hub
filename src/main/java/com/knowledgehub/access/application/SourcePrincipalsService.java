package com.knowledgehub.access.application;

import com.knowledgehub.access.domain.AccessGraphEdgeKind;
import com.knowledgehub.access.domain.AccessGraphNodeKind;
import com.knowledgehub.access.domain.DefaultPolicy;
import com.knowledgehub.access.domain.PermissionOrigin;
import com.knowledgehub.access.domain.Principal;
import com.knowledgehub.access.domain.PrincipalType;
import com.knowledgehub.access.domain.port.GrantRepository;
import com.knowledgehub.access.domain.port.PrincipalRepository;
import com.knowledgehub.access.domain.port.SystemConfigRepository;
import com.knowledgehub.knowledge.ingestion.application.SourceNotFoundException;
import com.knowledgehub.knowledge.ingestion.domain.port.SourceRepository;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves which principals can read a source — the inverse of {@link
 * PrincipalAdminService#effectivePermissions}. A direct grantor's access flows down to every
 * principal that inherits through it, the same {@code MEMBER_OF} closure effective-permissions
 * walks upward.
 */
@Service
public class SourcePrincipalsService {

  private final PrincipalRepository principals;
  private final GrantRepository grants;
  private final SystemConfigRepository systemConfig;
  private final SourceRepository sources;

  public SourcePrincipalsService(
      PrincipalRepository principals,
      GrantRepository grants,
      SystemConfigRepository systemConfig,
      SourceRepository sources) {
    this.principals = principals;
    this.grants = grants;
    this.systemConfig = systemConfig;
    this.sources = sources;
  }

  @Transactional(readOnly = true)
  public SourcePrincipals resolve(String sourceId) {
    if (sources.findById(sourceId).isEmpty()) {
      throw new SourceNotFoundException(sourceId);
    }

    Map<String, Set<String>> viaByPrincipal = directAndInheritedVia(sourceId);
    boolean policyReadable =
        systemConfig.defaultPolicy() == DefaultPolicy.ALLOW
            && !grants.allGrantedSources().contains(sourceId);

    List<SourcePrincipals.PrincipalAccess> access = new ArrayList<>();
    for (Principal principal : principals.findAll()) {
      Set<String> via = viaByPrincipal.getOrDefault(principal.principalId(), Set.of());
      Optional<PermissionOrigin> origin = principal.originFor(via);
      if (origin.isEmpty() && policyReadable) {
        origin = Optional.of(PermissionOrigin.POLICY);
      }
      if (origin.isEmpty()) {
        continue; // not readable at all
      }
      access.add(new SourcePrincipals.PrincipalAccess(principal.principalId(), origin.get(), via));
    }
    return new SourcePrincipals(sourceId, access);
  }

  /**
   * The subgraph explaining who can read a source: every principal a grant reaches (directly or
   * through membership), the source itself, and the membership/grant edges among them. The
   * inverse of {@link PrincipalAdminService#accessGraph}. ADMIN- and POLICY-origin principals have
   * no edge to draw, so — like the principal-rooted graph never showing a role-bypassed source —
   * they don't appear here even though {@link #resolve} lists them.
   */
  @Transactional(readOnly = true)
  public AccessGraph accessGraph(String sourceId) {
    if (sources.findById(sourceId).isEmpty()) {
      throw new SourceNotFoundException(sourceId);
    }

    Set<String> reachedIds = directAndInheritedVia(sourceId).keySet();
    Map<String, Principal> byId =
        principals.findAll().stream()
            .collect(Collectors.toMap(Principal::principalId, principal -> principal));

    List<AccessGraph.Node> nodes = new ArrayList<>();
    for (String id : reachedIds) {
      Principal principal = byId.get(id);
      AccessGraphNodeKind kind =
          principal.type() == PrincipalType.GROUP ? AccessGraphNodeKind.GROUP : AccessGraphNodeKind.SUBJECT;
      nodes.add(new AccessGraph.Node(id, kind));
    }
    nodes.add(new AccessGraph.Node(sourceId, AccessGraphNodeKind.SOURCE));

    List<AccessGraph.Edge> edges = new ArrayList<>();
    for (String grantor : grants.directGrantorsOf(sourceId)) {
      edges.add(new AccessGraph.Edge(grantor, sourceId, AccessGraphEdgeKind.GRANT));
    }
    principals
        .membershipGraph()
        .forEach(
            (groupId, memberIds) -> {
              if (!reachedIds.contains(groupId)) {
                return;
              }
              for (String memberId : memberIds) {
                if (reachedIds.contains(memberId)) {
                  edges.add(new AccessGraph.Edge(groupId, memberId, AccessGraphEdgeKind.MEMBER));
                }
              }
            });

    return new AccessGraph(sourceId, nodes, edges);
  }

  /**
   * Direct grantors of the source, plus every principal that inherits through one — a downward
   * {@code MEMBER_OF} walk from each grantor group, the mirror of the upward walk
   * {@code grantingPrincipalsFor} does from one principal.
   */
  private Map<String, Set<String>> directAndInheritedVia(String sourceId) {
    Set<String> directGrantors = grants.directGrantorsOf(sourceId);
    Map<String, List<String>> membership = principals.membershipGraph();

    Map<String, Set<String>> via = new LinkedHashMap<>();
    for (String grantor : directGrantors) {
      via.computeIfAbsent(grantor, k -> new LinkedHashSet<>()).add(grantor);
      Deque<String> queue = new ArrayDeque<>(membership.getOrDefault(grantor, List.of()));
      Set<String> visited = new HashSet<>();
      while (!queue.isEmpty()) {
        String memberId = queue.poll();
        if (!visited.add(memberId)) {
          continue;
        }
        via.computeIfAbsent(memberId, k -> new LinkedHashSet<>()).add(grantor);
        queue.addAll(membership.getOrDefault(memberId, List.of()));
      }
    }
    return via;
  }
}
