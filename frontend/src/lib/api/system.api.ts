import { apiFetch, request } from './http.client'

/** Shape of GET /api/v1/system/info (backend SystemInfo record). */
export interface SystemInfo {
  application: string
  version: string
  activeProfiles: string[]
}

const SYSTEM_INFO_PATH = '/api/v1/system/info'

/**
 * Validates a candidate connection by hitting an authenticated endpoint with the
 * given credentials directly (the connection is not in the store yet). A 200
 * means the base URL is reachable and the API key is accepted.
 */
export function validateConnection(baseUrl: string, apiKey: string): Promise<SystemInfo> {
  return request<SystemInfo>(baseUrl, apiKey, SYSTEM_INFO_PATH)
}

/** System info for the currently active connection. */
export function fetchSystemInfo(): Promise<SystemInfo> {
  return apiFetch<SystemInfo>(SYSTEM_INFO_PATH)
}
