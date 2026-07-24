import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

// isMock is read from import.meta.env at module load, so each test stubs the env
// then imports the module fresh.
async function loadApi() {
  vi.stubEnv('VITE_API_MODE', 'mock')
  return import('@/features/sources/api/sources.api')
}

describe('sources api in mock mode', () => {
  beforeEach(() => vi.resetModules())
  afterEach(() => vi.unstubAllEnvs())

  it('lists sources and finds one by id', async () => {
    const { fetchSources, fetchSource } = await loadApi()

    const all = await fetchSources()
    expect(all.length).toBeGreaterThan(0)

    const one = await fetchSource(all[1].id)
    expect(one.id).toBe(all[1].id)
  })

  it('fills the optional fields the server defaults when creating', async () => {
    const { createSource } = await loadApi()

    const created = await createSource({ id: 'new-src', type: 'FS', uriOrPath: '/srv/new' })

    expect(created).toMatchObject({ id: 'new-src', type: 'FS', uriOrPath: '/srv/new' })
    expect(created.include).toEqual([])
    expect(created.ignore).toEqual([])
    expect(created.ref).toBeNull()
  })

  it('merges a partial update onto the current source', async () => {
    const { fetchSources, updateSource } = await loadApi()
    const [first] = await fetchSources()

    const updated = await updateSource(first.id, { name: 'Renamed' })

    expect(updated.name).toBe('Renamed')
    // untouched fields keep their value
    expect(updated.uriOrPath).toBe(first.uriOrPath)
    expect(updated.include).toEqual(first.include)
  })

  it('reports sync and status against the requested source id', async () => {
    const { syncSource, fetchSourceStatus } = await loadApi()

    const result = await syncSource('product-docs')
    expect(result.sourceId).toBe('product-docs')
    expect(typeof result.idempotent).toBe('boolean')

    const status = await fetchSourceStatus('product-docs')
    expect(status.sourceId).toBe('product-docs')
    expect(status.indexed).toBe(true)
  })
})
