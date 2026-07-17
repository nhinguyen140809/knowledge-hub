import { createBrowserRouter, RouterProvider } from 'react-router-dom'
import { ConnectScreen } from '../features/auth/ConnectScreen'
import { PrivateRoute } from '../features/auth/PrivateRoute'
import { DashboardPage } from '../features/dashboard'
import { SourcesPage } from '../features/sources'
import { AppLayout } from '../shared/components/layout/AppLayout'
import { NotFoundPage } from '../shared/components/NotFoundPage'

const router = createBrowserRouter([
  // Public: reachable without an active backend connection.
  { path: '/connect', element: <ConnectScreen /> },
  // Private: the layout is guarded, its children render in the layout's <Outlet>.
  {
    element: (
      <PrivateRoute>
        <AppLayout />
      </PrivateRoute>
    ),
    children: [
      { path: '/', element: <DashboardPage /> },
      { path: '/sources', element: <SourcesPage /> },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
])

/** The app's router as a component, so the root composes <Providers><Router />. */
export function Router() {
  return <RouterProvider router={router} />
}
