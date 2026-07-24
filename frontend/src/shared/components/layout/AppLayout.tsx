import { Outlet, useLocation } from 'react-router-dom'
import { ErrorBoundary } from '@/shared/components/ErrorBoundary'
import { SidebarProvider } from '@/shared/components/ui/Sidebar'
import { findActiveLabel } from '@/shared/lib/navigation.utils'
import { AppHeader } from './AppHeader'
import { AppSidebar, NAV_ITEMS } from './AppSidebar'

/** Chrome shared by every authenticated screen: a push-style sidebar, a header
 *  with the page title, and an outlet for the routed page. */
export function AppLayout() {
  const { pathname } = useLocation()
  return (
    <SidebarProvider>
      <div className="bg-background text-foreground flex h-screen">
        <AppSidebar />
        <div className="flex min-w-0 flex-1 flex-col">
          <AppHeader title={findActiveLabel(NAV_ITEMS, pathname) || 'Knowledge Hub'} />
          {/* Stable gutter: the width never jumps when the scrollbar appears,
              so overflow-measuring children (tab lists) don't flicker. */}
          <main className="flex-1 scrollbar-gutter-stable overflow-y-auto p-6">
            {/* Keyed by path so a broken page doesn't stay broken after
                navigating away — a fresh key remounts with a clean state. */}
            <ErrorBoundary key={pathname}>
              <Outlet />
            </ErrorBoundary>
          </main>
        </div>
      </div>
    </SidebarProvider>
  )
}
