/** Data types a query can be restricted to. */
export type HitType = 'code' | 'doc' | 'requirement' | 'commit'

/**
 * Body of POST /query. Only `text` is required; the knobs narrow the search
 * *within* the caller's readable sources — they can never widen access, since
 * the ACL pre-filter is applied server-side on every path.
 */
export interface QueryInput {
  text: string
  /** Max results; omit for the server's configured top-k. */
  topK?: number | null
  sourceId?: string | null
  /** Version/branch; omit for the canonical ref. */
  ref?: string | null
  type?: HitType | null
}

/** Where a hit lives and how it was found — enough to cite it. */
export interface HitMetadata {
  /** 'chunk' or 'entity'. */
  kind: string
  sourceId: string
  path: string
  lineStart: number | null
  lineEnd: number | null
  type: string | null
  ref: string | null
  /** ISO-8601 timestamp, or null. */
  indexedAt: string | null
  commitSha: string | null
  /** Relationship types traversed for a graph-expanded hit. */
  viaPath: string[]
}

/** One ranked hit: the matched chunk/entity id, its fused score and metadata. */
export interface Hit {
  id: string
  relevanceScore: number
  metadata: HitMetadata
}

/** Response of POST /query. `servedFromCanonicalRef` is true when the requested
 *  ref was not indexed and the canonical ref was searched instead. */
export interface RankedResult {
  hits: Hit[]
  servedFromCanonicalRef: boolean
}
