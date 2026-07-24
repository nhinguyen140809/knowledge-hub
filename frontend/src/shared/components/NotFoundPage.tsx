import { Button } from '@heroui/react/button'
import { Link } from 'react-router-dom'

/** Catch-all page for unknown routes (the router's `path: '*'`). */
export function NotFoundPage() {
  return (
    <div className="bg-background text-foreground flex min-h-screen flex-col items-center justify-center gap-4 p-4">
      <p className="text-accent text-5xl font-extrabold">404</p>
      <p className="text-muted-foreground text-2xl font-semibold">Page not found</p>
      <Link to="/">
        <Button variant="primary" size="lg" className="mt-8">
          Go to dashboard
        </Button>
      </Link>
    </div>
  )
}
