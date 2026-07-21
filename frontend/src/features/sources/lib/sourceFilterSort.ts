import type { Source, SourceType } from '../types/source.type'

export type SourceTypeFilter = SourceType | 'ALL'
export type SourceSortField = 'lastUpdate' | 'id'
export type SourceSortDirection = 'asc' | 'desc'

export interface SourceFilterSortState {
  type: SourceTypeFilter
  sortField: SourceSortField
  sortDirection: SourceSortDirection
}

export const DEFAULT_SOURCE_FILTER_SORT: SourceFilterSortState = {
  type: 'ALL',
  sortField: 'lastUpdate',
  sortDirection: 'desc',
}

/** Filters by type, then sorts by id or last-update. Sources with no
 *  timestamp (never synced) always sort last, regardless of direction —
 *  there's nothing to compare them by. */
export function applySourceFilterSort(sources: Source[], state: SourceFilterSortState): Source[] {
  const filtered = state.type === 'ALL' ? sources : sources.filter((s) => s.type === state.type)
  const sign = state.sortDirection === 'asc' ? 1 : -1

  return [...filtered].sort((a, b) => {
    if (state.sortField === 'id') return sign * a.id.localeCompare(b.id)

    if (!a.updatedAt && !b.updatedAt) return 0
    if (!a.updatedAt) return 1
    if (!b.updatedAt) return -1
    return sign * (new Date(a.updatedAt).getTime() - new Date(b.updatedAt).getTime())
  })
}
