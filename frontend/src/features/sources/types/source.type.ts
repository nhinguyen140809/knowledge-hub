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
}
