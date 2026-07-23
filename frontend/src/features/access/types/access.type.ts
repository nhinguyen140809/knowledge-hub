/** A principal is either an individual subject or a group of principals. */
export type PrincipalType = 'SUBJECT' | 'GROUP'

/** Role decides admin rights; ADMIN unlocks every /admin endpoint. */
export type Role = 'ADMIN' | 'MEMBER'

/** System-wide fallback when no grant matches: deny everything or allow everything. */
export type DefaultPolicy = 'DENY' | 'ALLOW'
export const ALLOW: DefaultPolicy = 'ALLOW'
export const DENY: DefaultPolicy = 'DENY'

/** JSON view of a principal — mirrors the backend PrincipalResponse. */
export interface Principal {
  principalId: string
  type: PrincipalType
  role: Role
}

/** Body of POST /admin/principals. */
export interface CreatePrincipalInput {
  principalId: string
  type: PrincipalType
  role: Role
}

/** Credential metadata for management/audit. Never carries the secret or hash;
 *  revoke is a soft-delete, so revoked credentials still appear in the list. */
export interface Credential {
  credentialId: string
  name: string
  revoked: boolean
  /** ISO-8601 instant. */
  createdAt: string
  /** ISO-8601 instant, or null when never used. */
  lastUsedAt: string | null
}

/** Response of POST /admin/principals/{id}/credentials. The `secret` is returned
 *  exactly once and is never retrievable again — surface it immediately. */
export interface IssuedCredential {
  credentialId: string
  name: string
  secret: string
}

/** Where a readable source's access comes from, precedence in this order when
 *  several apply — a direct grant is the only one revocable from the
 *  principal's own panel. */
export type GrantOrigin = 'DIRECT' | 'INHERITED' | 'POLICY'

/** One readable source with its provenance. `via` lists every principal (self
 *  or group) whose grant reaches it; empty for POLICY. */
export interface EffectiveSource {
  sourceId: string
  origin: GrantOrigin
  via: string[]
}

/**
 * A principal's resolved read access, one entry per readable source. Mirrors
 * the effective-permissions response.
 */
export interface EffectivePermissions {
  principalId: string
  defaultPolicy: DefaultPolicy
  sources: EffectiveSource[]
}

/** Body of POST /admin/grants and POST /admin/grants/revoke. */
export interface GrantInput {
  principalId: string
  sourceIds: string[]
}

/**
 * Every principal plus the membership edges between them. Membership maps a
 * group id to its direct member ids; a member may be a subject or another group,
 * and may appear under several groups, so this is a directed graph rather than a
 * tree.
 */
export interface PrincipalGraph {
  principals: Principal[]
  membership: Record<string, string[]>
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

/**
 * The subgraph explaining one principal's access, render-ready: the focus
 * principal, its transitive groups, the sources they reach, and the edges
 * between them. Mirrors GET /admin/principals/{id}/access-graph. No
 * positions — layout belongs to the client.
 */
export interface PrincipalAccessGraph {
  focus: string
  nodes: AccessGraphNode[]
  edges: AccessGraphEdge[]
}
