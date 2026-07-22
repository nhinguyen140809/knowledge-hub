import { apiFetch } from '@/lib/api/axios'
import { mockResolve } from '@/lib/api/mocks/mock.util'
import { isMock } from '@/lib/config'
import { DENY, type DefaultPolicy } from '../types/access.type'

const DEFAULT_POLICY = '/admin/default-policy'

/** GET /admin/default-policy */
export function fetchDefaultPolicy(): Promise<DefaultPolicy> {
  if (isMock) return mockResolve(DENY)
  return apiFetch<{ policy: DefaultPolicy }>(DEFAULT_POLICY).then((r) => r.policy)
}

/** PUT /admin/default-policy — echoes the new policy back. */
export function setDefaultPolicy(policy: DefaultPolicy): Promise<DefaultPolicy> {
  if (isMock) return mockResolve(policy)
  return apiFetch<{ policy: DefaultPolicy }>(DEFAULT_POLICY, {
    method: 'PUT',
    data: { policy },
  }).then((r) => r.policy)
}
