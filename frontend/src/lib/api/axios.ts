import axios, { type AxiosError, type AxiosRequestConfig } from 'axios'
import type { ProblemDetail } from '@/shared/types/response.type'
import { getActiveConnection, useConnectionStore } from '../store/connections.store'

/** Every backend serves its REST API under this prefix (see WebConfig on the
 *  server). Centralised here so callers pass resource paths like '/sources'. */
const API_PREFIX = '/api/v1'

/** A non-2xx response from a backend. `status` is 0 for client-side failures
 *  (no active connection, network error, timeout); `code` is the server's
 *  ProblemDetail error code when present. */
export class ApiError extends Error {
  readonly status: number
  readonly code?: string

  constructor(status: number, message: string, code?: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
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
      baseURL: `${baseUrl.replace(/\/+$/, '')}${API_PREFIX}`,
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

/** Normalise any thrown value (axios or otherwise) into an ApiError, preferring
 *  the server's ProblemDetail `detail`/`code` over the generic axios message. */
function toApiError(err: unknown): ApiError {
  if (axios.isAxiosError(err)) {
    const axiosErr = err as AxiosError<Partial<ProblemDetail>>
    const status = axiosErr.response?.status ?? 0
    const problem = axiosErr.response?.data
    const message = problem?.detail || axiosErr.response?.statusText || axiosErr.message
    return new ApiError(status, message, problem?.code)
  }
  return new ApiError(0, err instanceof Error ? err.message : 'Unknown error')
}
