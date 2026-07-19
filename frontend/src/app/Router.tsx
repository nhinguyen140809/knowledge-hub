import { createBrowserRouter, RouterProvider } from 'react-router-dom'
import { ConnectPage, PrivateRoute } from '../features/auth'
import { AccessPage } from '../features/access'
import { DashboardPage } from '../features/dashboard'
import { QueryPage } from '../features/query'
import { SourceDetailPage, SourcesPage } from '../features/sources'
import { AppLayout } from '../shared/components/layout/AppLayout'
import { NotFoundPage } from '../shared/components/NotFoundPage'

const router = createBrowserRouter([
  // Public: reachable without an active backend connection.
  { path: '/connect', element: <ConnectPage /> },
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
      { path: '/sources/:id', element: <SourceDetailPage /> },
      { path: '/access', element: <AccessPage /> },
      { path: '/query', element: <QueryPage /> },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
])

/** The app's router as a component, so the root composes <Providers><Router />. */
export function Router() {
  return <RouterProvider router={router} />
}
