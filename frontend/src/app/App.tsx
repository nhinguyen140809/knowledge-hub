import { Providers } from './Providers'
import { Router } from './Router'

/** Root component: app-wide providers wrapping the router. */
export function App() {
  return (
    <Providers>
      <Router />
    </Providers>
  )
}
