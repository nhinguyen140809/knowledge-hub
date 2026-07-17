import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  getActiveConnection,
  useConnectionKeys,
  useConnectionStore,
} from '@/lib/store/connections.store'

function reset() {
  useConnectionStore.setState({ connections: [], activeId: null })
  useConnectionKeys.setState({ keys: {} })
  localStorage.clear()
  sessionStorage.clear()
}

describe('connections store', () => {
  beforeEach(reset)

  it('adds a connection and makes it active', () => {
    useConnectionStore
      .getState()
      .addConnection({ label: 'Local', baseUrl: 'http://a', apiKey: 'k1' })

    const state = useConnectionStore.getState()
    expect(state.connections).toHaveLength(1)
    expect(state.activeId).toBe(state.connections[0].id)
    expect(getActiveConnection(state)?.baseUrl).toBe('http://a')
  })

  it('reuses the entry with the same baseUrl instead of duplicating', () => {
    const { addConnection } = useConnectionStore.getState()
    addConnection({ label: 'One', baseUrl: 'http://a', apiKey: 'k1' })
    addConnection({ label: 'Two', baseUrl: 'http://a', apiKey: 'k2' })

    const state = useConnectionStore.getState()
    expect(state.connections).toHaveLength(1)
    expect(state.connections[0].label).toBe('Two')
    expect(state.connections[0].apiKey).toBe('k2')
  })

  it('falls back to the first remaining connection when the active one is removed', () => {
    const { addConnection } = useConnectionStore.getState()
    addConnection({ label: 'A', baseUrl: 'http://a', apiKey: 'k1' })
    addConnection({ label: 'B', baseUrl: 'http://b', apiKey: 'k2' }) // active

    const activeId = useConnectionStore.getState().activeId ?? ''
    useConnectionStore.getState().removeConnection(activeId)

    const state = useConnectionStore.getState()
    expect(state.connections).toHaveLength(1)
    expect(state.connections[0].label).toBe('A')
    expect(state.activeId).toBe(state.connections[0].id)
  })

  it('clears the active connection when the last one is removed', () => {
    const { addConnection } = useConnectionStore.getState()
    addConnection({ label: 'A', baseUrl: 'http://a', apiKey: 'k1' })

    const activeId = useConnectionStore.getState().activeId ?? ''
    useConnectionStore.getState().removeConnection(activeId)

    expect(useConnectionStore.getState().activeId).toBeNull()
  })

  it('keeps apiKey out of localStorage and only in sessionStorage', () => {
    useConnectionStore
      .getState()
      .addConnection({ label: 'A', baseUrl: 'http://a', apiKey: 'secret' })

    const persisted = JSON.parse(localStorage.getItem('kh.connections') ?? '{}')
    const storedConnection = persisted.state.connections[0]
    expect(storedConnection.baseUrl).toBe('http://a')
    expect(storedConnection.apiKey).toBeUndefined()

    const sessionRaw = JSON.parse(sessionStorage.getItem('kh.connection-keys') ?? '{}')
    expect(Object.values(sessionRaw.state.keys)).toContain('secret')
  })
})

/** Simulates a page load: storages are pre-seeded, then the store module is
 *  imported fresh. Both storages are synchronous, so zustand must rehydrate
 *  during create() — the assertions run without waiting for anything. */
describe('rehydration on fresh load', () => {
  const storedConnections = JSON.stringify({
    state: {
      connections: [{ id: 'c1', label: 'A', baseUrl: 'http://a' }],
      activeId: 'c1',
    },
    version: 0,
  })

  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
    vi.resetModules()
  })

  it('merges apiKeys back from sessionStorage (page reload in same tab)', async () => {
    localStorage.setItem('kh.connections', storedConnections)
    sessionStorage.setItem(
      'kh.connection-keys',
      JSON.stringify({ state: { keys: { c1: 'secret' } }, version: 0 }),
    )

    const fresh = await import('@/lib/store/connections.store')
    const state = fresh.useConnectionStore.getState()
    expect(state.activeId).toBe('c1')
    expect(state.connections[0].apiKey).toBe('secret')
  })

  it('yields an empty apiKey when sessionStorage is gone (tab was closed)', async () => {
    localStorage.setItem('kh.connections', storedConnections)

    const fresh = await import('@/lib/store/connections.store')
    const state = fresh.useConnectionStore.getState()
    expect(state.connections[0].apiKey).toBe('')
    expect(fresh.getActiveConnection(state)?.apiKey).toBe('')
  })
})
