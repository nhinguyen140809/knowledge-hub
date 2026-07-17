import { Link } from 'react-router-dom'

/** Catch-all page for unknown routes (the router's `path: '*'`). */
export function NotFoundPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-2 bg-neutral-50 text-center dark:bg-neutral-950">
      <p className="text-4xl font-semibold text-neutral-900 dark:text-neutral-100">404</p>
      <p className="text-neutral-500">Trang không tồn tại.</p>
      <Link to="/" className="text-blue-600 underline dark:text-blue-400">
        Về trang chủ
      </Link>
    </div>
  )
}
