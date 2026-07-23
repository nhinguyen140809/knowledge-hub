import { apiFetch } from '@/lib/api/axios'
import { mockResolve } from '@/lib/api/mocks/mock.util'
import { isMock } from '@/lib/config'
import type { GrantInput } from '../types/access.type'

const GRANTS = '/admin/grants'

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
