import { type ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useActiveConnection } from '@/lib/store/connections.store'

/** Guards routes that need a usable backend connection: one that is active AND
 *  still has its apiKey. Keys live in sessionStorage, so after the tab closes the
 *  connection list survives but the key is gone — this catches that "reopened,
 *  key missing" case too and sends the user back to reconnect. */
export function PrivateRoute({ children }: { children: ReactNode }) {
  const active = useActiveConnection()
  if (!active?.apiKey) return <Navigate to="/connect" replace />
  return <>{children}</>
}
