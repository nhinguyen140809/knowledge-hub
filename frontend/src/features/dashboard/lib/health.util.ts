import type { SystemInfo } from '@/lib/api/system.api'

/** Maps 1:1 to HeroUI Chip's semantic colors, so the panel can spread it. */
export type HealthTone = 'success' | 'warning' | 'danger'

export interface HealthStatus {
  label: string
  tone: HealthTone
}

/**
 * Derives the dashboard's health badge from the backend's system info. What
 * "healthy" means is a product policy with several defensible answers, so it
 * lives in one hook rather than being scattered through the UI.
 */
const NON_PROD_PROFILES = ['dev', 'test', 'local', 'staging']

export function deriveHealthStatus(info: SystemInfo): HealthStatus {
  // Reachability is already proven (only called with data), so these are the
  // "connected, but worth flagging" cases, most actionable first.
  const nonProd = info.activeProfiles.some((p) => NON_PROD_PROFILES.includes(p.toLowerCase()))
  if (nonProd) return { label: 'Non-production', tone: 'warning' }
  if (!info.version || info.version === 'unknown') {
    return { label: 'Unknown build', tone: 'warning' }
  }
  return { label: 'Healthy', tone: 'success' }
}
