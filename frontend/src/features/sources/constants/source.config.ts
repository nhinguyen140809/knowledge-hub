import type { SourceType } from '../types/source.type'

/** Chip color per source type — GIT stands out as the more common/primary case. */
export const SOURCE_TYPE_COLOR: Record<SourceType, 'accent' | 'default'> = {
  GIT: 'accent',
  FS: 'default',
}

/** Human-facing label per source type, for display in place of the raw enum value. */
export const SOURCE_TYPE_LABEL: Record<SourceType, string> = {
  GIT: 'Git',
  FS: 'Folder',
}

interface SourceTypeLocationConfig {
  /** Whether this type has a git ref (branch/tag/commit) — GIT only. */
  hasRef: boolean
  /** Label for the uriOrPath field in create/edit forms. */
  formLabel: string
  /** Placeholder for the uriOrPath field in create/edit forms. */
  formPlaceholder: string
  /** Label for the uriOrPath row in the read-only summary view. */
  summaryLabel: string
}

/** Everything about a source type that depends on whether it's a git
 *  repository or a filesystem folder — keeps that branching out of components. */
export const SOURCE_TYPE_LOCATION: Record<SourceType, SourceTypeLocationConfig> = {
  GIT: {
    hasRef: true,
    formLabel: 'Repository URL',
    formPlaceholder: 'https://github.com/acme/wiki.git',
    summaryLabel: 'Repository',
  },
  FS: {
    hasRef: false,
    formLabel: 'Folder path',
    formPlaceholder: '/srv/knowledge/docs',
    summaryLabel: 'Path',
  },
}
