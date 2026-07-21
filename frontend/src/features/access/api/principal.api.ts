import { apiFetch } from '@/lib/api/axios'
import { mockResolve } from '@/lib/api/mocks/mock.util'
import { isMock } from '@/lib/config'
import type {
  CreatePrincipalInput,
  EffectivePermissions,
  Principal,
  PrincipalGraph,
} from '../types/access.type'
import {
  mockEffectivePermissions,
  mockMembers,
  mockPrincipalGraph,
  mockPrincipals,
} from './access.mock'

const PRINCIPALS = '/admin/principals'

/** Principal/member ids are user-chosen, so escape them before URL use. */
const enc = encodeURIComponent

// ---------------------------------------------------------------- principals

/** GET /admin/principals */
export function fetchPrincipals(): Promise<Principal[]> {
  if (isMock) return mockResolve(mockPrincipals)
  return apiFetch<Principal[]>(PRINCIPALS)
}

/** GET /admin/principals/graph — principals plus membership edges in one call. */
export function fetchPrincipalGraph(): Promise<PrincipalGraph> {
  if (isMock) return mockResolve(mockPrincipalGraph)
  return apiFetch<PrincipalGraph>(`${PRINCIPALS}/graph`)
}

/** GET /admin/principals/{id} */
export function fetchPrincipal(id: string): Promise<Principal> {
  if (isMock) {
    return mockResolve(mockPrincipals.find((p) => p.principalId === id) ?? mockPrincipals[0])
  }
  return apiFetch<Principal>(`${PRINCIPALS}/${enc(id)}`)
}

/** POST /admin/principals — returns the created principal (201). */
export function createPrincipal(input: CreatePrincipalInput): Promise<Principal> {
  if (isMock) return mockResolve(input)
  return apiFetch<Principal>(PRINCIPALS, { method: 'POST', data: input })
}

/** DELETE /admin/principals/{id} — 204. */
export function deletePrincipal(id: string): Promise<void> {
  if (isMock) return mockResolve(undefined)
  return apiFetch<void>(`${PRINCIPALS}/${enc(id)}`, { method: 'DELETE' })
}

// ------------------------------------------------------------- group members

/** GET /admin/principals/{id}/members — ids of a group's direct members. */
export function fetchMembers(groupId: string): Promise<string[]> {
  if (isMock) return mockResolve(mockMembers[groupId] ?? [])
  return apiFetch<string[]>(`${PRINCIPALS}/${enc(groupId)}/members`)
}

/** POST /admin/principals/{id}/members — 204. */
export function addMember(groupId: string, memberId: string): Promise<void> {
  if (isMock) return mockResolve(undefined)
  return apiFetch<void>(`${PRINCIPALS}/${enc(groupId)}/members`, {
    method: 'POST',
    data: { memberId },
  })
}

/** DELETE /admin/principals/{id}/members/{memberId} — 204. */
export function removeMember(groupId: string, memberId: string): Promise<void> {
  if (isMock) return mockResolve(undefined)
  return apiFetch<void>(`${PRINCIPALS}/${enc(groupId)}/members/${enc(memberId)}`, {
    method: 'DELETE',
  })
}

/** GET /admin/principals/{id}/effective-permissions — grants resolved through
 *  group membership and the default policy. */
export function fetchEffectivePermissions(principalId: string): Promise<EffectivePermissions> {
  if (isMock) return mockResolve({ ...mockEffectivePermissions, principalId })
  return apiFetch<EffectivePermissions>(`${PRINCIPALS}/${enc(principalId)}/effective-permissions`)
}
