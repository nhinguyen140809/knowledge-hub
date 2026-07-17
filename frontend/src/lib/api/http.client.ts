import { getActiveConnection, useConnectionStore } from '../store/connections.store'

/** A non-2xx response from a backend. `status` is 0 for client-side failures
 *  (no active connection, network error). */
export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

/** Low-level request against an explicit backend — used to validate a candidate
 *  connection before it is stored. */
export async function request<T>(
  baseUrl: string,
  apiKey: string,
  path: string,
  init?: RequestInit,
): Promise<T> {
  const res = await fetch(`${baseUrl}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${apiKey}`,
      ...init?.headers,
    },
  })
  if (!res.ok) throw new ApiError(res.status, `${res.status} ${res.statusText}`)
  if (res.status === 204) return undefined as T
  return (await res.json()) as T
}

/** Request against the currently active backend. Reads the connection outside
 *  React via getState(), so it works from any query/mutation function. */
export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const conn = getActiveConnection(useConnectionStore.getState())
  if (!conn) throw new ApiError(0, 'Chưa chọn backend nào')
  return request<T>(conn.baseUrl, conn.apiKey, path, init)
}
