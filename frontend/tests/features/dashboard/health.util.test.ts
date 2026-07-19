import { describe, expect, it } from 'vitest'
import { deriveHealthStatus } from '@/features/dashboard/lib/health.util'
import type { SystemInfo } from '@/shared/types/system.type'

const info = (over: Partial<SystemInfo> = {}): SystemInfo => ({
  application: 'knowledge-hub',
  version: '1.0.0',
  activeProfiles: ['prod'],
  ...over,
})

describe('deriveHealthStatus', () => {
  it('is healthy on a production build with a known version', () => {
    expect(deriveHealthStatus(info())).toEqual({ label: 'Healthy', tone: 'success' })
  })

  it('warns when pointed at a non-production profile', () => {
    expect(deriveHealthStatus(info({ activeProfiles: ['dev'] }))).toEqual({
      label: 'Non-production',
      tone: 'warning',
    })
    // case-insensitive, and flags even when mixed with prod
    expect(deriveHealthStatus(info({ activeProfiles: ['PROD', 'Staging'] })).tone).toBe('warning')
  })

  it('warns on an unknown build when the profile looks production-like', () => {
    expect(deriveHealthStatus(info({ version: 'unknown', activeProfiles: [] }))).toEqual({
      label: 'Unknown build',
      tone: 'warning',
    })
  })

  it('prioritises the non-production warning over an unknown build', () => {
    expect(deriveHealthStatus(info({ version: 'unknown', activeProfiles: ['dev'] })).label).toBe(
      'Non-production',
    )
  })
})
