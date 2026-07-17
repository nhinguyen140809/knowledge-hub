import { apiFetch } from '@/lib/api/axios'
import { mockResolve } from '@/lib/api/mocks/mock.util'
import { isMock } from '@/lib/config'
import type { Source } from '../types/source.type'
import { mockSources } from './sources.mock'

/** GET /admin/sources for the currently active connection. */
export function fetchSources(): Promise<Source[]> {
  if (isMock) return mockResolve(mockSources)
  return apiFetch<Source[]>('/admin/sources')
}
