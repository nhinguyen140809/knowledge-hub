import { createBrowserRouter, RouterProvider } from 'react-router-dom'
import { ConnectPage, PrivateRoute } from '../features/auth'
import { AccessPage, AccessSourcesPage } from '../features/access'
import { DashboardPage } from '../features/dashboard'
import { HelpPage } from '../features/help'
import { QueryPage } from '../features/query'
import { SourceDetailPage, SourcesPage } from '../features/sources'
import { NotFoundPage } from '../shared/components/NotFoundPage'
import { AuthenticatedApp } from './AuthenticatedApp'

const router = createBrowserRouter([
  // Public: reachable without an active backend connection.
  { path: '/connect', element: <ConnectPage /> },
  // Private: the layout is guarded, its children render in the layout's <Outlet>.
  {
    element: (
      <PrivateRoute>
        <AuthenticatedApp />
      </PrivateRoute>
    ),
    children: [
      { path: '/', element: <DashboardPage /> },
      { path: '/sources', element: <SourcesPage /> },
      { path: '/sources/:id', element: <SourceDetailPage /> },
      { path: '/access', element: <AccessPage /> },
      { path: '/access/sources', element: <AccessSourcesPage /> },
      { path: '/query', element: <QueryPage /> },
      { path: '/help', element: <HelpPage /> },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
])

/** The app's router as a component, so the root composes <Providers><Router />. */
export function Router() {
  return <RouterProvider router={router} />
}
