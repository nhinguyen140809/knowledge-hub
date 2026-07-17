import { createBrowserRouter, Navigate } from 'react-router-dom'
import { ConnectScreen } from '../features/auth/ConnectScreen'
import { RequireConnection } from '../features/auth/RequireConnection'
import { DashboardPage } from '../features/dashboard/DashboardPage'
import { AppLayout } from './AppLayout'

export const router = createBrowserRouter([
  { path: '/connect', element: <ConnectScreen /> },
  {
    element: (
      <RequireConnection>
        <AppLayout />
      </RequireConnection>
    ),
    children: [{ path: '/', element: <DashboardPage /> }],
  },
  { path: '*', element: <Navigate to="/" replace /> },
])
