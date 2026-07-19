import type { SystemInfo } from '@/shared/types/system.type'
import { isMock } from '../config'
import { apiFetch, request } from './axios'
import { mockResolve } from './mocks/mock.util'
import { mockSystemInfo } from './mocks/system.mock'

const SYSTEM_INFO_PATH = '/system/info'

/**
 * Validates a candidate connection by hitting an authenticated endpoint with the
 * given credentials directly (the connection is not in the store yet). A 200
 * means the base URL is reachable and the API key is accepted. In mock mode any
 * URL/key is accepted so the UI can be explored without a backend.
 */
export function validateConnection(baseUrl: string, apiKey: string): Promise<SystemInfo> {
  if (isMock) return mockResolve(mockSystemInfo)
  return request<SystemInfo>(baseUrl, apiKey, SYSTEM_INFO_PATH)
}

/** System info for the currently active connection. */
export function fetchSystemInfo(): Promise<SystemInfo> {
  if (isMock) return mockResolve(mockSystemInfo)
  return apiFetch<SystemInfo>(SYSTEM_INFO_PATH)
}
