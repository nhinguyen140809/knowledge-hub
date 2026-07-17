import { beforeEach, describe, expect, it } from 'vitest'
import { getActiveConnection, useConnectionStore } from './connections.store'

function reset() {
  useConnectionStore.setState({ connections: [], activeId: null })
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

    const sessionKeys = JSON.parse(sessionStorage.getItem('kh.connection-keys') ?? '{}')
    expect(Object.values(sessionKeys)).toContain('secret')
  })
})
