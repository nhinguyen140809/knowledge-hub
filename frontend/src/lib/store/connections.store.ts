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
const SESSION_KEYS = 'kh.connection-keys'

/** Non-secret fields — the only ones written to (long-lived) localStorage. */
type StoredConnection = Omit<Connection, 'apiKey'>

/** apiKeys live in sessionStorage keyed by connection id: they survive a page
 *  reload but are cleared when the tab/browser closes, and never hit localStorage. */
function loadSessionKeys(): Record<string, string> {
  try {
    return JSON.parse(sessionStorage.getItem(SESSION_KEYS) ?? '{}') as Record<string, string>
  } catch {
    return {}
  }
}

function saveSessionKeys(connections: Connection[]): void {
  const keys: Record<string, string> = {}
  for (const c of connections) keys[c.id] = c.apiKey
  sessionStorage.setItem(SESSION_KEYS, JSON.stringify(keys))
}

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
      // localStorage keeps only non-secret fields; apiKeys are stripped here.
      partialize: (state) => ({
        connections: state.connections.map(({ id, label, baseUrl }): StoredConnection => ({
          id,
          label,
          baseUrl,
        })),
        activeId: state.activeId,
      }),
      // On rehydrate, splice each apiKey back from sessionStorage (empty if the
      // tab was closed — the user reconnects to supply it again).
      merge: (persisted, current) => {
        const stored = persisted as { connections?: StoredConnection[]; activeId?: string | null }
        const keys = loadSessionKeys()
        const connections: Connection[] = (stored.connections ?? []).map((c) => ({
          ...c,
          apiKey: keys[c.id] ?? '',
        }))
        return { ...current, connections, activeId: stored.activeId ?? null }
      },
    },
  ),
)

// Mirror apiKeys to sessionStorage on every change, so a reload can restore them.
useConnectionStore.subscribe((state) => saveSessionKeys(state.connections))

/** Resolve the active backend outside React (used by the API client). */
export function getActiveConnection(state: ConnectionState): Connection | null {
  return state.connections.find((c) => c.id === state.activeId) ?? null
}

/** React hook for the active backend. */
export function useActiveConnection(): Connection | null {
  return useConnectionStore((s) => s.connections.find((c) => c.id === s.activeId) ?? null)
}
