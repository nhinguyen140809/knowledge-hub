import { apiFetch } from '@/lib/api/axios'
import { mockResolve } from '@/lib/api/mocks/mock.util'
import { isMock } from '@/lib/config'
import type {
  CreateSourceInput,
  Source,
  SourceStatus,
  SyncResult,
  UpdateSourceInput,
} from '../types/source.type'
import { mockSources, mockStatusFor, mockSyncResult } from './sources.mock'

const BASE = '/admin/sources'

/** Source ids are user-chosen, so they must be escaped before going into a URL. */
const path = (id: string, suffix = '') => `${BASE}/${encodeURIComponent(id)}${suffix}`

/** GET /admin/sources */
export function fetchSources(): Promise<Source[]> {
  if (isMock) return mockResolve(mockSources)
  return apiFetch<Source[]>(BASE)
}

/** GET /admin/sources/{id} */
export function fetchSource(id: string): Promise<Source> {
  if (isMock) return mockResolve(mockSources.find((s) => s.id === id) ?? mockSources[0])
  return apiFetch<Source>(path(id))
}

/** POST /admin/sources — returns the created source (201). */
export function createSource(input: CreateSourceInput): Promise<Source> {
  if (isMock) {
    return mockResolve({
      ref: null,
      include: [],
      ignore: [],
      name: null,
      description: null,
      updatedAt: null,
      ...input,
    })
  }
  return apiFetch<Source>(BASE, { method: 'POST', data: input })
}

/** PATCH /admin/sources/{id} — partial update, returns the updated source. */
export function updateSource(id: string, input: UpdateSourceInput): Promise<Source> {
  if (isMock) {
    const current = mockSources.find((s) => s.id === id) ?? mockSources[0]
    return mockResolve({ ...current, ...input })
  }
  return apiFetch<Source>(path(id), { method: 'PATCH', data: input })
}

/** DELETE /admin/sources/{id} — 204 No Content. */
export function deleteSource(id: string): Promise<void> {
  if (isMock) return mockResolve(undefined)
  return apiFetch<void>(path(id), { method: 'DELETE' })
}

/** POST /admin/sources/{id}/sync — idempotent; re-syncing an unchanged source
 *  is a no-op flagged by `idempotent`. */
export function syncSource(id: string): Promise<SyncResult> {
  if (isMock) return mockResolve({ ...mockSyncResult, sourceId: id })
  return apiFetch<SyncResult>(path(id, '/sync'), { method: 'POST' })
}

/** GET /admin/sources/{id}/status — index freshness. */
export function fetchSourceStatus(id: string): Promise<SourceStatus> {
  if (isMock) return mockResolve(mockStatusFor(id))
  return apiFetch<SourceStatus>(path(id, '/status'))
}
