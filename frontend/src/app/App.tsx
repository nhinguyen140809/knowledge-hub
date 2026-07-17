import { RouterProvider } from 'react-router-dom'
import { router } from './app.router'
import { AppProviders } from './AppProviders'

/** Root component: app-wide providers wrapping the router. */
export function App() {
  return (
    <AppProviders>
      <RouterProvider router={router} />
    </AppProviders>
  )
}
