import { create } from 'zustand'
import { persist } from 'zustand/middleware'

/** One backend instance the admin UI can talk to. */
export interface Connection {
  id: string
  label: string
  baseUrl: string
  apiKey: string
}

export interface ConnectionState {
  connections: Connection[]
  activeId: string | null
  /** Add a backend (or update the one with the same baseUrl) and make it active. */
  addConnection: (input: Omit<Connection, 'id'>) => void
  /** Forget a backend; if it was active, fall back to the first remaining one. */
  removeConnection: (id: string) => void
  /** Switch which backend subsequent requests target. */
  setActive: (id: string) => void
}

const STORAGE_KEY = 'kh.connections'

export const useConnectionStore = create<ConnectionState>()(
  persist(
    (set) => ({
      connections: [],
      activeId: null,
      addConnection: (input) =>
        set((state) => {
          const existing = state.connections.find((c) => c.baseUrl === input.baseUrl)
          if (existing) {
            return {
              connections: state.connections.map((c) =>
                c.id === existing.id ? { ...existing, ...input, id: existing.id } : c,
              ),
              activeId: existing.id,
            }
          }
          const id = crypto.randomUUID()
          return { connections: [...state.connections, { ...input, id }], activeId: id }
        }),
      removeConnection: (id) =>
        set((state) => {
          const connections = state.connections.filter((c) => c.id !== id)
          const activeId = state.activeId === id ? (connections[0]?.id ?? null) : state.activeId
          return { connections, activeId }
        }),
      setActive: (id) => set({ activeId: id }),
    }),
    {
      name: STORAGE_KEY,
      partialize: persistConnections,
    },
  ),
)

/**
 * Decides what part of the connection registry is written to localStorage and
 * restored on the next visit.
 *
 * TODO(human): implement the persistence policy. Every field you return here is
 * stored in the browser's localStorage in plaintext — including each backend's
 * `apiKey`, which is an admin bearer token. Weigh convenience (reopen the tab and
 * your instances are still there, ready to use) against exposure (any XSS on this
 * page can read localStorage and exfiltrate every stored admin key). Options to
 * consider: persist everything; persist connections but strip `apiKey` (user
 * re-enters the key each session); or keep only non-secret fields and hold keys
 * in sessionStorage/memory. Return the object shape you want persisted.
 */
function persistConnections(state: ConnectionState): Partial<ConnectionState> {
  return { connections: state.connections, activeId: state.activeId }
}

/** Resolve the active backend outside React (used by the API client). */
export function getActiveConnection(state: ConnectionState): Connection | null {
  return state.connections.find((c) => c.id === state.activeId) ?? null
}

/** React hook for the active backend. */
export function useActiveConnection(): Connection | null {
  return useConnectionStore((s) => s.connections.find((c) => c.id === s.activeId) ?? null)
}
