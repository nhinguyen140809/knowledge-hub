import {
  DENY,
  type AccessGraphEdge,
  type AccessGraphNode,
  type Credential,
  type EffectivePermissions,
  type IssuedCredential,
  type Principal,
  type PrincipalAccessGraph,
  type PrincipalGraph,
} from '../types/access.type'

export const mockPrincipals: Principal[] = [
  { principalId: 'eng-team', type: 'GROUP', role: 'MEMBER' },
  { principalId: 'support-team', type: 'GROUP', role: 'MEMBER' },
  { principalId: 'alice', type: 'SUBJECT', role: 'ADMIN' },
  { principalId: 'bob', type: 'SUBJECT', role: 'MEMBER' },
  { principalId: 'carol', type: 'SUBJECT', role: 'MEMBER' },
]

/** Exercises the shapes the tree has to survive: a nested group (support-team
 *  inside eng-team) and a principal in two groups at once (carol). alice is
 *  deliberately in no group: admins stay out of the membership graph — their
 *  access is total by role, so membership would only mislead. */
export const mockMembers: Record<string, string[]> = {
  'eng-team': ['support-team', 'carol'],
  'support-team': ['bob', 'carol'],
}

export const mockPrincipalGraph: PrincipalGraph = {
  principals: mockPrincipals,
  membership: mockMembers,
}

export const mockCredentials: Credential[] = [
  {
    credentialId: 'cred_01H9ZA',
    name: 'laptop',
    revoked: false,
    createdAt: '2026-06-01T09:15:00Z',
    lastUsedAt: '2026-07-17T07:55:12Z',
  },
  {
    credentialId: 'cred_01H9ZB',
    name: 'ci-pipeline',
    revoked: false,
    createdAt: '2026-06-14T11:02:30Z',
    lastUsedAt: null,
  },
  {
    credentialId: 'cred_01H9ZC',
    name: 'old-laptop',
    revoked: true,
    createdAt: '2026-01-08T16:40:00Z',
    lastUsedAt: '2026-05-30T10:11:00Z',
  },
]

export const mockIssuedCredential: IssuedCredential = {
  credentialId: 'cred_01H9ZD',
  name: 'new-credential',
  secret: 'kh_sk_mock_5f3a9c1e7b2d4086a1c3e5f7b9d0a2c4',
}

/** Direct grants per principal. Deliberately different per principal so that
 *  switching selection visibly changes the graph and the sources panel, and
 *  together with membership it still exercises every origin they distinguish:
 *  direct-only, direct + inherited at once (bob and engineering-wiki),
 *  inherited via one group, and inherited via two groups at once
 *  (incident-runbooks for bob and carol). */
export const mockDirectGrants: Record<string, string[]> = {
  bob: ['engineering-wiki', 'product-docs'],
  alice: ['product-docs'],
  carol: [],
  'eng-team': ['engineering-wiki', 'design-assets', 'incident-runbooks'],
  'support-team': ['support-macros', 'incident-runbooks'],
}

/** Self plus every group reachable from the principal by walking membership
 *  upward — the `(p)-[:MEMBER_OF*0..]->(g)` closure both resolvers share. */
function membershipClosure(principalId: string): Set<string> {
  const via = new Set<string>([principalId])
  const queue = [principalId]
  while (queue.length > 0) {
    const current = queue.pop()!
    for (const [groupId, memberIds] of Object.entries(mockMembers)) {
      if (memberIds.includes(current) && !via.has(groupId)) {
        via.add(groupId)
        queue.push(groupId)
      }
    }
  }
  return via
}

/** Resolves effective permissions the way the backend does: own grants plus
 *  grants of every group reachable through membership, transitively. */
export function mockResolveEffectivePermissions(principalId: string): EffectivePermissions {
  const via = membershipClosure(principalId)

  const grantedVia: Record<string, string[]> = {}
  for (const principal of via) {
    for (const sourceId of mockDirectGrants[principal] ?? []) {
      ;(grantedVia[sourceId] ??= []).push(principal)
    }
  }

  return {
    principalId,
    defaultPolicy: DENY,
    sources: Object.entries(grantedVia).map(([sourceId, viaPrincipals]) => ({
      sourceId,
      // POLICY never occurs here: the mock default policy is DENY, so
      // everything readable got that way through some grant.
      origin: viaPrincipals.includes(principalId) ? ('DIRECT' as const) : ('INHERITED' as const),
      via: viaPrincipals,
    })),
  }
}

/** The scoped subgraph GET .../access-graph returns: the focus principal, its
 *  transitive groups, their sources, and only the edges among those. */
export function mockResolveAccessGraph(principalId: string): PrincipalAccessGraph {
  const closure = membershipClosure(principalId)
  const typeById = new Map(mockPrincipals.map((p) => [p.principalId, p.type]))

  const nodes: AccessGraphNode[] = [...closure].map((id) => ({
    id,
    kind: typeById.get(id) ?? 'SUBJECT',
  }))
  const edges: AccessGraphEdge[] = []

  for (const [groupId, memberIds] of Object.entries(mockMembers)) {
    if (!closure.has(groupId)) continue
    for (const memberId of memberIds) {
      if (closure.has(memberId)) edges.push({ from: groupId, to: memberId, kind: 'MEMBER' })
    }
  }

  const sourceIds = new Set<string>()
  for (const principal of closure) {
    for (const sourceId of mockDirectGrants[principal] ?? []) {
      sourceIds.add(sourceId)
      edges.push({ from: principal, to: sourceId, kind: 'GRANT' })
    }
  }
  for (const id of sourceIds) nodes.push({ id, kind: 'SOURCE' })

  return { focus: principalId, nodes, edges }
}
