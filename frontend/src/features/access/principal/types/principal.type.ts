import type {
  AccessGraphEdge,
  AccessGraphNode,
  DefaultPolicy,
  GrantOrigin,
  Principal,
  PrincipalType,
  Role,
} from '../../shared/types/access.type'

/** Body of POST /admin/principals. When `parentGroupId` is set, the principal
 *  is created directly inside that group in one atomic step — creating and
 *  linking never half-succeed. Incompatible with role ADMIN (admins stay out
 *  of the membership graph). */
export interface CreatePrincipalInput {
  principalId: string
  type: PrincipalType
  role: Role
  parentGroupId?: string
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

/** A credential from the cross-principal list (GET /admin/credentials), tagged
 *  with the principal it belongs to since that isn't implied by the URL there. */
export interface GlobalCredential extends Credential {
  principalId: string
}

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
