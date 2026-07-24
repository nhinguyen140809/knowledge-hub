import type { Source } from '../types/source.type'

/**
 * The source business rules in one place — domain derivations components would
 * otherwise re-compute inline. Presentation (colors, labels, icons per type)
 * stays in the component config; only the domain breakdown lives here.
 */

/** Headline breakdown of the source collection for the section summary. The
 *  Git/filesystem split is the one fact not obvious from scanning the list, so
 *  it is surfaced alongside the total. */
export function summarizeSources(sources: Source[]): {
  total: number
  git: number
  filesystem: number
} {
  return {
    total: sources.length,
    git: sources.filter((s) => s.type === 'GIT').length,
    filesystem: sources.filter((s) => s.type === 'FS').length,
  }
}
