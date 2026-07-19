import { Chip, Separator } from '@heroui/react'
import { CircleHelp, Database, KeyRound, LayoutDashboard, LogOut, Search } from 'lucide-react'
import { useLocation } from 'react-router-dom'
import { ConnectionSwitcher } from '@/features/auth'
import { isMock } from '@/lib/config'
import { useConnectionStore } from '@/lib/store/connections.store'
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarMenuSub,
} from '@/shared/components/ui/Sidebar'
import { isActivePath } from '@/shared/lib/navigation.utils'
import { renderLink } from '@/shared/lib/renderLink'
import { type NavItem } from '@/shared/types/navigation.type'

/** The app's navigation entries — the data this sidebar renders. */
// eslint-disable-next-line react-refresh/only-export-components
export const NAV_ITEMS: NavItem[] = [
  { label: 'Dashboard', to: '/', icon: LayoutDashboard, match: 'exact' },
  { label: 'Sources', to: '/sources', icon: Database },
  { label: 'Access control', to: '/access', icon: KeyRound },
  { label: 'Query', to: '/query', icon: Search },
]

/** The app's sidebar: nav from NAV_ITEMS, app name + backend switcher in the
 *  header, Help/Log out in the footer. All rows are ui/Sidebar primitives. */
export function AppSidebar() {
  const { pathname } = useLocation()
  const activeId = useConnectionStore((s) => s.activeId)
  const disconnect = useConnectionStore((s) => s.disconnect)

  return (
    <Sidebar>
      <SidebarHeader>
        <div className="flex items-center gap-2">
          <span className="text-foreground text-sm font-semibold">Knowledge Hub</span>
          {isMock && (
            <Chip color="warning" size="sm" variant="soft">
              MOCK
            </Chip>
          )}
        </div>
        <ConnectionSwitcher />
      </SidebarHeader>

      <SidebarContent>
        <SidebarMenu>
          {NAV_ITEMS.map((item) =>
            item.children ? (
              <SidebarMenuItem key={item.label}>
                <SidebarMenuSub
                  label={
                    <>
                      <item.icon size={16} />
                      {item.label}
                    </>
                  }
                  defaultExpanded={item.children.some((c) => isActivePath(pathname, c.to, c.match))}
                >
                  {item.children.map((child) => (
                    <SidebarMenuItem key={child.to}>
                      <SidebarMenuButton
                        size="sm"
                        isActive={isActivePath(pathname, child.to, child.match)}
                        render={renderLink(child.to)}
                      >
                        {child.label}
                      </SidebarMenuButton>
                    </SidebarMenuItem>
                  ))}
                </SidebarMenuSub>
              </SidebarMenuItem>
            ) : (
              <SidebarMenuItem key={item.to}>
                <SidebarMenuButton
                  isActive={isActivePath(pathname, item.to, item.match)}
                  render={renderLink(item.to)}
                >
                  <item.icon size={16} />
                  {item.label}
                </SidebarMenuButton>
              </SidebarMenuItem>
            ),
          )}
        </SidebarMenu>
      </SidebarContent>

      <SidebarFooter>
        <Separator className="mb-2" />
        <SidebarMenuButton>
          <CircleHelp size={16} />
          Help
        </SidebarMenuButton>
        <SidebarMenuButton onPress={() => activeId && disconnect(activeId)}>
          <LogOut size={16} />
          Log out
        </SidebarMenuButton>
      </SidebarFooter>
    </Sidebar>
  )
}
