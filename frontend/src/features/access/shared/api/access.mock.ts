import type { Principal } from '../types/access.type'

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
 *  upward — the `(p)-[:MEMBER_OF*0..]->(g)` closure both the principal- and
 *  source-side resolvers share. */
export function membershipClosure(principalId: string): Set<string> {
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

/** Every principal reachable downward from a direct grantor by walking
 *  membership the other way — the mirror of `membershipClosure`, which walks
 *  the same `mockMembers` edges upward from a principal instead. */
export function reachableFrom(grantorId: string): Set<string> {
  const reached = new Set<string>([grantorId])
  const queue = [grantorId]
  while (queue.length > 0) {
    const current = queue.pop()!
    for (const memberId of mockMembers[current] ?? []) {
      if (!reached.has(memberId)) {
        reached.add(memberId)
        queue.push(memberId)
      }
    }
  }
  return reached
}
