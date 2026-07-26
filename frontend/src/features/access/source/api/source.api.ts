import { apiFetch } from '@/lib/api/axios'
import { mockResolve } from '@/lib/api/mocks/mock.util'
import { isMock } from '@/lib/config'
import type { SourceAccessGraph, SourcePrincipals } from '../types/source.type'
import { mockResolveSourceAccessGraph, mockResolveSourcePrincipals } from './source.mock'

const SOURCES = '/admin/sources'

/** Source ids are user-chosen, so escape them before URL use. */
const enc = encodeURIComponent

/** GET /admin/sources/{id}/principals — every principal that can read this
 *  source, with its access origin. The inverse of effective-permissions. */
export function fetchSourcePrincipals(sourceId: string): Promise<SourcePrincipals> {
  if (isMock) return mockResolve(mockResolveSourcePrincipals(sourceId))
  return apiFetch<SourcePrincipals>(`${SOURCES}/${enc(sourceId)}/principals`)
}

/** GET /admin/sources/{id}/access-graph — the scoped subgraph explaining who
 *  can read this source. */
export function fetchSourceAccessGraph(sourceId: string): Promise<SourceAccessGraph> {
  if (isMock) return mockResolve(mockResolveSourceAccessGraph(sourceId))
  return apiFetch<SourceAccessGraph>(`${SOURCES}/${enc(sourceId)}/access-graph`)
}
