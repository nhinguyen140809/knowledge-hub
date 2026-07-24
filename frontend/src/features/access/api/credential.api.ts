import { apiFetch } from '@/lib/api/axios'
import { mockResolve } from '@/lib/api/mocks/mock.util'
import { isMock } from '@/lib/config'
import type { Credential, IssuedCredential } from '../types/access.type'
import { mockCredentials, mockIssuedCredential } from './access.mock'

const PRINCIPALS = '/admin/principals'
const CREDENTIALS = '/admin/credentials'

/** Principal/credential ids are user-chosen, so escape them before URL use. */
const enc = encodeURIComponent

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
