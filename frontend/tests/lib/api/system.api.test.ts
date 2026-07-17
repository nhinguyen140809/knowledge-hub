import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

// isMock is read from import.meta.env at module load, so each test stubs the env
// then imports the module fresh.
describe('system api in mock mode', () => {
  beforeEach(() => vi.resetModules())
  afterEach(() => vi.unstubAllEnvs())

  it('accepts any credentials and returns mock system info', async () => {
    vi.stubEnv('VITE_API_MODE', 'mock')
    const { validateConnection } = await import('@/lib/api/system.api')

    const info = await validateConnection('http://anything', 'anykey')

    expect(info.application).toContain('mock')
    expect(info.activeProfiles).toContain('mock')
  })
})
