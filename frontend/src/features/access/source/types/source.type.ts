import type { AccessGraphEdge, AccessGraphNode, GrantOrigin } from '../../shared/types/access.type'

/** One principal that can read a source, with its provenance. The same
 *  `GrantOrigin`/`via` shape as `EffectiveSource`, with principal and source
 *  swapped. */
export interface SourcePrincipal {
  principalId: string
  origin: GrantOrigin
  via: string[]
}

/** Every principal that can read one source — the inverse of
 *  `EffectivePermissions`, resolved from the source's side. */
export interface SourcePrincipals {
  sourceId: string
  principals: SourcePrincipal[]
}

/**
 * The subgraph explaining who can read one source: the source, every
 * principal a grant reaches (directly or through membership), and the edges
 * between them. Mirrors GET /admin/sources/{id}/access-graph — the inverse of
 * `PrincipalAccessGraph`, resolved from the source's side.
 */
export interface SourceAccessGraph {
  focus: string
  nodes: AccessGraphNode[]
  edges: AccessGraphEdge[]
}
