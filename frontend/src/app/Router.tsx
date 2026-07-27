import { createBrowserRouter, RouterProvider } from 'react-router-dom'
import { ConnectionsPage, ConnectPage, PrivateRoute } from '../features/auth'
import { AccessPrincipalsPage, AccessSourcesPage } from '../features/access'
import { DashboardPage } from '../features/dashboard'
import { HelpPage } from '../features/help'
import { QueryPage } from '../features/query'
import { SourceDetailPage, SourcesPage } from '../features/sources'
import { NotFoundPage } from '../shared/components/NotFoundPage'
import { ROUTES } from '../shared/constants'
import { AuthenticatedApp } from './AuthenticatedApp'

const router = createBrowserRouter([
  // Public: reachable without an active backend connection.
  { path: ROUTES.connect, element: <ConnectPage /> },
  // Private: the layout is guarded, its children render in the layout's <Outlet>.
  {
    element: (
      <PrivateRoute>
        <AuthenticatedApp />
      </PrivateRoute>
    ),
    children: [
      { path: ROUTES.dashboard, element: <DashboardPage /> },
      { path: ROUTES.connections, element: <ConnectionsPage /> },
      { path: ROUTES.sources, element: <SourcesPage /> },
      { path: '/sources/:id', element: <SourceDetailPage /> },
      { path: ROUTES.accessPrincipals, element: <AccessPrincipalsPage /> },
      { path: ROUTES.accessSources, element: <AccessSourcesPage /> },
      { path: ROUTES.query, element: <QueryPage /> },
      { path: ROUTES.help, element: <HelpPage /> },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
])

/** The app's router as a component, so the root composes <Providers><Router />. */
export function Router() {
  return <RouterProvider router={router} />
}
