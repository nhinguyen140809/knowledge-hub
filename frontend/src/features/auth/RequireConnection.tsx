import { type ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useConnectionStore } from '../../lib/store/connections.store'

/** Route guard: without an active backend connection, bounce to the connect screen. */
export function RequireConnection({ children }: { children: ReactNode }) {
  const activeId = useConnectionStore((s) => s.activeId)
  if (!activeId) return <Navigate to="/connect" replace />
  return <>{children}</>
}
