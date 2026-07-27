import {
  mockDirectGrants,
  mockMembers,
  mockPrincipals,
  reachableFrom,
} from '../../shared/api/access.mock'
import type { AccessGraphEdge, AccessGraphNode } from '../../shared/types/access.type'
import type { SourceAccessGraph, SourcePrincipal, SourcePrincipals } from '../types/source.type'

/** Resolves which principals can read a source: every direct grantor, every
 *  principal that inherits through group membership, and every admin (role
 *  bypasses grants) not already reached some other way. The inverse of the
 *  principal side's effective permissions, resolved from the source's side. */
export function mockResolveSourcePrincipals(sourceId: string): SourcePrincipals {
  const grantors = Object.entries(mockDirectGrants)
    .filter(([, sourceIds]) => sourceIds.includes(sourceId))
    .map(([principalId]) => principalId)

  const via: Record<string, string[]> = {}
  for (const grantor of grantors) {
    for (const principalId of reachableFrom(grantor)) {
      ;(via[principalId] ??= []).push(grantor)
    }
  }

  const principals: SourcePrincipal[] = Object.entries(via).map(([principalId, viaGrantors]) => ({
    principalId,
    // Same rule as the backend: a principal is its own grantor only when it
    // appears among the ids that reached it.
    origin: viaGrantors.includes(principalId) ? 'DIRECT' : 'INHERITED',
    via: viaGrantors,
  }))

  // Mock default policy is DENY, so the only principals left to add are
  // admins bypassing grants entirely.
  for (const principal of mockPrincipals) {
    if (principal.role === 'ADMIN' && !via[principal.principalId]) {
      principals.push({ principalId: principal.principalId, origin: 'ADMIN', via: [] })
    }
  }

  return { sourceId, principals }
}

/**
 * The subgraph GET .../sources/{id}/access-graph returns: the source, every
 * principal a grant reaches (directly or through membership), and the edges
 * among them. ADMIN/POLICY-origin principals have no edge to draw, so — like
 * the principal-rooted graph never showing a role-bypassed source — they
 * don't appear here even though `mockResolveSourcePrincipals` lists them.
 */
export function mockResolveSourceAccessGraph(sourceId: string): SourceAccessGraph {
  const grantors = Object.entries(mockDirectGrants)
    .filter(([, sourceIds]) => sourceIds.includes(sourceId))
    .map(([principalId]) => principalId)

  const reached = new Set<string>()
  for (const grantor of grantors) {
    for (const id of reachableFrom(grantor)) reached.add(id)
  }

  const typeById = new Map(mockPrincipals.map((p) => [p.principalId, p.type]))
  const nodes: AccessGraphNode[] = [...reached].map((id) => ({
    id,
    kind: typeById.get(id) ?? 'SUBJECT',
  }))
  nodes.push({ id: sourceId, kind: 'SOURCE' })

  const edges: AccessGraphEdge[] = grantors.map((grantor) => ({
    from: grantor,
    to: sourceId,
    kind: 'GRANT' as const,
  }))
  for (const [groupId, memberIds] of Object.entries(mockMembers)) {
    if (!reached.has(groupId)) continue
    for (const memberId of memberIds) {
      if (reached.has(memberId)) edges.push({ from: groupId, to: memberId, kind: 'MEMBER' })
    }
  }

  return { focus: sourceId, nodes, edges }
}
