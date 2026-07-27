import { request } from '@/lib/api/axios'
import { mockResolve } from '@/lib/api/mocks/mock.util'
import { mockSystemInfo } from '@/lib/api/mocks/system.mock'
import { isMock } from '@/lib/config'
import type { SystemInfo } from '@/shared/types/system.type'

/**
 * Validates a candidate connection by calling an authenticated endpoint with the
 * given credentials directly — the connection is not in the store yet, so this
 * uses `request` rather than `apiFetch`. A 200 means the base URL is reachable
 * and the API key is accepted. In mock mode any URL/key is accepted so the UI can
 * be explored without a backend.
 */
export function validateConnection(baseUrl: string, apiKey: string): Promise<SystemInfo> {
  if (isMock) return mockResolve(mockSystemInfo)
  return request<SystemInfo>(baseUrl, apiKey, '/system/info')
}
