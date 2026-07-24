import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

// isMock is read from import.meta.env at module load, so each test stubs the env
// then imports the module fresh.
describe('auth api in mock mode', () => {
  beforeEach(() => vi.resetModules())
  afterEach(() => vi.unstubAllEnvs())

  it('accepts any credentials and returns mock system info', async () => {
    vi.stubEnv('VITE_API_MODE', 'mock')
    const { validateConnection } = await import('@/features/auth/api/auth.api')

    const info = await validateConnection('http://anything', 'anykey')

    expect(info.productName.toLowerCase()).toContain('mock')
    expect(info.activeProfiles).toContain('mock')
  })
})
