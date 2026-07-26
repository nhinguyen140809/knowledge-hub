import { mockSources } from '@/features/sources/api/sources.mock'
import {
  membershipClosure,
  mockDirectGrants,
  mockMembers,
  mockPrincipals,
} from '../../shared/api/access.mock'
import { DENY, type AccessGraphEdge, type AccessGraphNode } from '../../shared/types/access.type'
import type {
  Credential,
  EffectivePermissions,
  EffectiveSource,
  GlobalCredential,
  IssuedCredential,
  PrincipalAccessGraph,
  PrincipalGraph,
} from '../types/principal.type'

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

/** The cross-principal list — same rows as `mockCredentials`, each attributed
 *  to an owner so the command palette and a future dashboard can navigate
 *  straight to the principal that holds it. */
export const mockGlobalCredentials: GlobalCredential[] = [
  { ...mockCredentials[0], principalId: 'bob' },
  { ...mockCredentials[1], principalId: 'alice' },
  { ...mockCredentials[2], principalId: 'bob' },
]

export const mockIssuedCredential: IssuedCredential = {
  credentialId: 'cred_01H9ZD',
  name: 'new-credential',
  secret: 'kh_sk_mock_5f3a9c1e7b2d4086a1c3e5f7b9d0a2c4',
}

/** Resolves effective permissions: own grants plus grants of every group
 *  reachable through membership, transitively — and for an ADMIN, every
 *  source regardless of grants (role bypasses them). */
export function mockResolveEffectivePermissions(principalId: string): EffectivePermissions {
  const via = membershipClosure(principalId)

  const grantedVia: Record<string, string[]> = {}
  for (const principal of via) {
    for (const sourceId of mockDirectGrants[principal] ?? []) {
      ;(grantedVia[sourceId] ??= []).push(principal)
    }
  }

  const sources: EffectiveSource[] = Object.entries(grantedVia).map(
    ([sourceId, viaPrincipals]) => ({
      sourceId,
      // POLICY never occurs here: the mock default policy is DENY, so
      // everything readable got that way through some grant (or the ADMIN
      // rows added below).
      origin: viaPrincipals.includes(principalId) ? 'DIRECT' : 'INHERITED',
      via: viaPrincipals,
    }),
  )

  // An admin reads everything; sources it reaches only through its role get
  // the ADMIN origin, while its real grants above keep theirs (still real,
  // revocable edges). Mock-to-mock import: both files exist only in mock
  // mode, and duplicating the source list here would just let them drift.
  const isAdmin = mockPrincipals.some((p) => p.principalId === principalId && p.role === 'ADMIN')
  if (isAdmin) {
    for (const source of mockSources) {
      if (!grantedVia[source.id]) sources.push({ sourceId: source.id, origin: 'ADMIN', via: [] })
    }
  }

  return { principalId, defaultPolicy: DENY, sources }
}

/** The scoped subgraph GET .../access-graph returns: the focus principal, its
 *  transitive groups, their sources, and only the edges among those. */
export function mockResolvePrincipalAccessGraph(principalId: string): PrincipalAccessGraph {
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
