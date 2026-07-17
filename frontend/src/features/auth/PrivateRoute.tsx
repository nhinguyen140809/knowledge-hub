import { type ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useConnectionStore } from '../../lib/store/connections.store'

/** Guards routes that need an active backend connection; ConnectScreen is the
 *  public route users land on without one. */
export function PrivateRoute({ children }: { children: ReactNode }) {
  const activeId = useConnectionStore((s) => s.activeId)
  if (!activeId) return <Navigate to="/connect" replace />
  return <>{children}</>
}
