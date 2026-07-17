import { Outlet } from 'react-router-dom'
import { ConnectionSwitcher } from '../../features/auth/ConnectionSwitcher'

/** Chrome shared by every authenticated screen: a header with the active-backend
 *  switcher, and an outlet for the routed page. */
export function AppLayout() {
  return (
    <div className="min-h-screen bg-neutral-50 text-neutral-900 dark:bg-neutral-950 dark:text-neutral-100">
      <header className="flex items-center justify-between border-b border-neutral-200 px-6 py-3 dark:border-neutral-800">
        <span className="font-semibold">Knowledge Hub - Admin</span>
        <ConnectionSwitcher />
      </header>
      <main className="p-6">
        <Outlet />
      </main>
    </div>
  )
}
