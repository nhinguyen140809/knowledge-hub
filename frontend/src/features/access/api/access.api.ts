import { apiFetch } from '@/lib/api/axios'
import { mockResolve } from '@/lib/api/mocks/mock.util'
import { isMock } from '@/lib/config'
import type {
  CreatePrincipalInput,
  Credential,
  DefaultPolicy,
  EffectivePermissions,
  GrantInput,
  IssuedCredential,
  Principal,
  PrincipalGraph,
} from '../types/access.type'
import {
  mockCredentials,
  mockEffectivePermissions,
  mockGrantedSources,
  mockIssuedCredential,
  mockMembers,
  mockPrincipalGraph,
  mockPrincipals,
} from './access.mock'

const PRINCIPALS = '/admin/principals'
const CREDENTIALS = '/admin/credentials'
const GRANTS = '/admin/grants'
const DEFAULT_POLICY = '/admin/default-policy'

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

// --------------------------------------------------------------- credentials

/** GET /admin/principals/{id}/credentials — metadata only, no secrets. */
export function fetchCredentials(principalId: string): Promise<Credential[]> {
  if (isMock) return mockResolve(mockCredentials)
  return apiFetch<Credential[]>(`${PRINCIPALS}/${enc(principalId)}/credentials`)
}

/** POST /admin/principals/{id}/credentials — the response carries the secret
 *  once; it is never stored and cannot be fetched again. */
export function issueCredential(principalId: string, name: string): Promise<IssuedCredential> {
  if (isMock) return mockResolve({ ...mockIssuedCredential, name })
  return apiFetch<IssuedCredential>(`${PRINCIPALS}/${enc(principalId)}/credentials`, {
    method: 'POST',
    data: { name },
  })
}

/** GET /admin/credentials — every credential across principals. */
export function fetchAllCredentials(): Promise<Credential[]> {
  if (isMock) return mockResolve(mockCredentials)
  return apiFetch<Credential[]>(CREDENTIALS)
}

/** DELETE /admin/credentials/{id} — soft-delete (revoke), 204. */
export function revokeCredential(credentialId: string): Promise<void> {
  if (isMock) return mockResolve(undefined)
  return apiFetch<void>(`${CREDENTIALS}/${enc(credentialId)}`, { method: 'DELETE' })
}

// -------------------------------------------------------------------- grants

/** GET /admin/grants/{principalId} — directly granted source ids (no inherited). */
export function fetchGrants(principalId: string): Promise<string[]> {
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

/** GET /admin/principals/{id}/effective-permissions — grants resolved through
 *  group membership and the default policy. */
export function fetchEffectivePermissions(principalId: string): Promise<EffectivePermissions> {
  if (isMock) return mockResolve({ ...mockEffectivePermissions, principalId })
  return apiFetch<EffectivePermissions>(`${PRINCIPALS}/${enc(principalId)}/effective-permissions`)
}

// ------------------------------------------------------------ default policy

/** GET /admin/default-policy */
export function fetchDefaultPolicy(): Promise<DefaultPolicy> {
  if (isMock) return mockResolve<DefaultPolicy>('DENY')
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
