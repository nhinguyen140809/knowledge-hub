import { Outlet, useLocation } from 'react-router-dom'
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
          <main className="flex-1 overflow-y-auto p-6">
            <Outlet />
          </main>
        </div>
      </div>
    </SidebarProvider>
  )
}
