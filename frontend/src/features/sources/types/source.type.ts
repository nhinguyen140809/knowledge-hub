/** A configured source: a Git repository or a filesystem folder. */
export type SourceType = 'GIT' | 'FS'

/** Response view of a configured source — mirrors the backend SourceResponse. */
export interface Source {
  id: string
  type: SourceType
  /** Repository URL (GIT) or folder path (FS). */
  uriOrPath: string
  /** Git ref (branch/tag/commit); null for FS or when unset. */
  ref: string | null
  /** Glob patterns to include; empty means "everything". */
  include: string[]
  /** Glob patterns to ignore. */
  ignore: string[]
  name: string | null
  description: string | null
  /** ISO-8601 timestamp of the last sync, or null if never synced. */
  updatedAt: string | null
}

/** Body of POST /admin/sources. `include`/`ignore` default to empty server-side;
 *  `ref` is Git-only; `name`/`description` are optional metadata. */
export interface CreateSourceInput {
  id: string
  type: SourceType
  uriOrPath: string
  ref?: string | null
  include?: string[]
  ignore?: string[]
  name?: string | null
  description?: string | null
}

/**
 * Body of PATCH /admin/sources/{id} — a partial update with merge semantics:
 * an omitted field keeps its current value, `[]` clears a glob list, a non-empty
 * array replaces it, and a blank name/description clears it. At least one field
 * must be present or the server rejects the request. id/type/location are fixed.
 */
export interface UpdateSourceInput {
  ref?: string | null
  include?: string[]
  ignore?: string[]
  name?: string | null
  description?: string | null
}

/** Result of POST /admin/sources/{id}/sync. `idempotent` is true when nothing
 *  changed, so a re-trigger can be distinguished from a real update. */
export interface SyncResult {
  sourceId: string
  indexed: number
  reindexed: number
  evicted: number
  skipped: number
  commitsIndexed: number
  durationMs: number
  /** Commit synced to, or null for a non-git source. */
  toCommit: string | null
  idempotent: boolean
}

/** GET /admin/sources/{id}/status — index freshness. When `indexed` is false the
 *  source has never been synced and every other field is null. */
export interface SourceStatus {
  sourceId: string
  indexed: boolean
  /** ISO-8601 timestamp of the last sync, or null. */
  indexedAt: string | null
  commitSha: string | null
  ref: string | null
}
