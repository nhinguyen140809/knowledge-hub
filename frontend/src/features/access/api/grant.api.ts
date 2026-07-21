import { apiFetch } from '@/lib/api/axios'
import { mockResolve } from '@/lib/api/mocks/mock.util'
import { isMock } from '@/lib/config'
import type { GrantInput } from '../types/access.type'
import { mockGrantedSources } from './access.mock'

const GRANTS = '/admin/grants'

/** Principal ids are user-chosen, so escape them before URL use. */
const enc = encodeURIComponent

/** GET /admin/grants/{principalId} — directly granted source ids (no inherited). */
export function fetchDirectGrants(principalId: string): Promise<string[]> {
  if (isMock) return mockResolve(mockGrantedSources)
  return apiFetch<string[]>(`${GRANTS}/${enc(principalId)}`)
}

/** POST /admin/grants — 204. */
export function grantSources(input: GrantInput): Promise<void> {
  if (isMock) return mockResolve(undefined)
  return apiFetch<void>(GRANTS, { method: 'POST', data: input })
}

/** POST /admin/grants/revoke — 204. */
export function revokeSources(input: GrantInput): Promise<void> {
  if (isMock) return mockResolve(undefined)
  return apiFetch<void>(`${GRANTS}/revoke`, { method: 'POST', data: input })
}
