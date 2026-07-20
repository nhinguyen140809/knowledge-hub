import { Avatar, Chip, Separator } from '@heroui/react'
import { CircleHelp, Database, KeyRound, LayoutDashboard, LogOut, Search } from 'lucide-react'
import { useLocation } from 'react-router-dom'
import { ConnectionSwitcher } from '@/features/auth'
import { isMock } from '@/lib/config'
import { useConnectionStore } from '@/lib/store/connections.store'
import { ThemeToggle } from '@/shared/components/theme/ThemeToggle'
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

function AppLogo() {
  return (
    <Avatar>
      <Avatar.Fallback>KH</Avatar.Fallback>
    </Avatar>
  )
}

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
          <AppLogo />
          <div className="flex flex-col gap-1">
            <span className="text-accent text-xl font-extrabold">Knowledge Hub</span>
            <div className="flex gap-2">
              {isMock && (
                <Chip color="warning" size="md" variant="soft">
                  MOCK
                </Chip>
              )}
            </div>
          </div>
        </div>
        <ConnectionSwitcher />
      </SidebarHeader>

      <Separator orientation="horizontal" />

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
        <ThemeToggle />
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
