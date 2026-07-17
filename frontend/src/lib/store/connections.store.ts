import { create } from 'zustand'
import { createJSONStorage, persist } from 'zustand/middleware'

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

/** Non-secret fields — the only ones written to (long-lived) localStorage. */
type StoredConnection = Omit<Connection, 'apiKey'>

const STORAGE_KEY = 'kh.connections'
const SESSION_KEYS = 'kh.connection-keys'

interface KeyState {
  keys: Record<string, string>
  set: (id: string, apiKey: string) => void
  drop: (id: string) => void
}

/**
 * apiKeys, kept apart from the connection registry and persisted to
 * sessionStorage (survives a reload, cleared when the tab/browser closes). Its
 * own store lets zustand's persist + createJSONStorage handle the serialization,
 * so no raw sessionStorage/JSON access is needed. Created before the connection
 * store so its keys are already rehydrated when that store's merge runs.
 */
export const useConnectionKeys = create<KeyState>()(
  persist(
    (set) => ({
      keys: {},
      set: (id, apiKey) => set((s) => ({ keys: { ...s.keys, [id]: apiKey } })),
      drop: (id) =>
        set((s) => {
          const keys = { ...s.keys }
          delete keys[id]
          return { keys }
        }),
    }),
    {
      name: SESSION_KEYS,
      storage: createJSONStorage(() => sessionStorage),
      partialize: (state) => ({ keys: state.keys }),
    },
  ),
)

export const useConnectionStore = create<ConnectionState>()(
  persist(
    (set, get) => ({
      connections: [],
      activeId: null,
      addConnection: (input) => {
        const existing = get().connections.find((c) => c.baseUrl === input.baseUrl)
        const id = existing ? existing.id : crypto.randomUUID()
        useConnectionKeys.getState().set(id, input.apiKey)
        set((state) => ({
          connections: existing
            ? state.connections.map((c) => (c.id === id ? { ...c, ...input, id } : c))
            : [...state.connections, { ...input, id }],
          activeId: id,
        }))
      },
      removeConnection: (id) => {
        useConnectionKeys.getState().drop(id)
        set((state) => {
          const connections = state.connections.filter((c) => c.id !== id)
          const activeId = state.activeId === id ? (connections[0]?.id ?? null) : state.activeId
          return { connections, activeId }
        })
      },
      setActive: (id) => set({ activeId: id }),
    }),
    {
      name: STORAGE_KEY,
      // localStorage keeps only non-secret fields; apiKeys are stripped here.
      partialize: (state) => ({
        connections: state.connections.map(({ id, label, baseUrl }): StoredConnection => ({
          id,
          label,
          baseUrl,
        })),
        activeId: state.activeId,
      }),
      // On rehydrate, splice each apiKey back from the sessionStorage-backed key
      // store (empty if the tab was closed — the user reconnects to supply it).
      merge: (persisted, current) => {
        const stored = persisted as { connections?: StoredConnection[]; activeId?: string | null }
        const { keys } = useConnectionKeys.getState()
        const connections: Connection[] = (stored.connections ?? []).map((c) => ({
          ...c,
          apiKey: keys[c.id] ?? '',
        }))
        return { ...current, connections, activeId: stored.activeId ?? null }
      },
    },
  ),
)

/** Resolve the active backend outside React (used by the API client). */
export function getActiveConnection(state: ConnectionState): Connection | null {
  return state.connections.find((c) => c.id === state.activeId) ?? null
}

/** React hook for the active backend. */
export function useActiveConnection(): Connection | null {
  return useConnectionStore((s) => s.connections.find((c) => c.id === s.activeId) ?? null)
}
