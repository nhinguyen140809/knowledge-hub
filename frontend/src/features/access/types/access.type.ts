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

/**
 * A principal's resolved read access. `readableSources` is exactly what the
 * retrieval pre-filter applies; `grantedVia` maps a source id to the principals
 * (self or groups) that grant it.
 */
export interface EffectivePermissions {
  principalId: string
  defaultPolicy: DefaultPolicy
  readableSources: string[]
  grantedVia: Record<string, string[]>
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
