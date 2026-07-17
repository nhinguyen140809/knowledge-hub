import axios, { type AxiosError, type AxiosRequestConfig } from 'axios'
import { getActiveConnection, useConnectionStore } from '../store/connections.store'

/** A non-2xx response from a backend. `status` is 0 for client-side failures
 *  (no active connection, network error, timeout). */
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
  config?: AxiosRequestConfig,
): Promise<T> {
  try {
    const res = await axios.request<T>({
      baseURL: baseUrl,
      url: path,
      headers: { Authorization: `Bearer ${apiKey}` },
      ...config,
    })
    return res.data
  } catch (err) {
    throw toApiError(err)
  }
}

/** Request against the currently active backend. Reads the connection outside
 *  React via getState(), so it works from any query/mutation function. */
export async function apiFetch<T>(path: string, config?: AxiosRequestConfig): Promise<T> {
  const conn = getActiveConnection(useConnectionStore.getState())
  if (!conn) throw new ApiError(0, 'Chưa chọn backend nào')
  return request<T>(conn.baseUrl, conn.apiKey, path, config)
}

/** Normalise any thrown value (axios or otherwise) into an ApiError. */
function toApiError(err: unknown): ApiError {
  if (axios.isAxiosError(err)) {
    const axiosErr = err as AxiosError
    const status = axiosErr.response?.status ?? 0
    return new ApiError(status, axiosErr.response?.statusText || axiosErr.message)
  }
  return new ApiError(0, err instanceof Error ? err.message : 'Unknown error')
}
