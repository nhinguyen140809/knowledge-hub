import { apiFetch } from '@/lib/api/axios'
import { mockResolve } from '@/lib/api/mocks/mock.util'
import { mockSystemInfo } from '@/lib/api/mocks/system.mock'
import { isMock } from '@/lib/config'
import type { SystemInfo } from '@/shared/types/system.type'

/** GET /system/info for the currently active connection. */
export function fetchSystemInfo(): Promise<SystemInfo> {
  if (isMock) return mockResolve(mockSystemInfo)
  return apiFetch<SystemInfo>('/system/info')
}
