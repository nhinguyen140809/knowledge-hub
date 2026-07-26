/** A principal is either an individual subject or a group of principals. */
export type PrincipalType = 'SUBJECT' | 'GROUP'

/** Role decides admin rights; ADMIN unlocks every /admin endpoint. */
export type Role = 'ADMIN' | 'MEMBER'

/** System-wide fallback when no grant matches: deny everything or allow everything. */
export type DefaultPolicy = 'DENY' | 'ALLOW'
export const ALLOW: DefaultPolicy = 'ALLOW'
export const DENY: DefaultPolicy = 'DENY'

/** JSON view of a principal. Referenced from both sides: the principal
 *  features build on it directly, and the source side reads `.type`/`.role`
 *  off it too (to label a graph node, and to spot an ADMIN bypassing grants). */
export interface Principal {
  principalId: string
  type: PrincipalType
  role: Role
}

/** Where a readable source's access comes from, precedence in this order when
 *  several apply — a direct grant is the only one revocable from the
 *  principal's own panel. ADMIN marks sources readable purely because the
 *  principal's role bypasses grants; grant-based origins still win for an
 *  admin's own grants, since those remain real, revocable edges. */
export type GrantOrigin = 'DIRECT' | 'INHERITED' | 'ADMIN' | 'POLICY'

/** Body of POST /admin/grants and POST /admin/grants/revoke. */
export interface GrantInput {
  principalId: string
  sourceIds: string[]
}

export type AccessGraphNodeKind = PrincipalType | 'SOURCE'

export interface AccessGraphNode {
  id: string
  kind: AccessGraphNodeKind
}

/** MEMBER runs group → member, GRANT runs principal → source. */
export interface AccessGraphEdge {
  from: string
  to: string
  kind: 'MEMBER' | 'GRANT'
}
