import type { SystemInfo } from '../system.api'

/** Stand-in for GET /api/v1/system/info when VITE_API_MODE=mock. */
export const mockSystemInfo: SystemInfo = {
  application: 'knowledge-hub (mock)',
  version: '0.0.0-mock',
  activeProfiles: ['mock'],
}
