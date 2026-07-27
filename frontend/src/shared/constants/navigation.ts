import { Workflow, Database, KeyRound, LayoutDashboard, Search } from 'lucide-react'
import type { NavItem } from '@/shared/types/navigation.type'
import { ROUTES } from './routes'

/** The app's main navigation entries — the sidebar renders these. */
export const NAV_ITEMS: NavItem[] = [
  { label: 'Dashboard', to: ROUTES.dashboard, icon: LayoutDashboard, match: 'exact' },
  { label: 'Sources', to: ROUTES.sources, icon: Database },
  {
    label: 'Access control',
    icon: KeyRound,
    children: [
      { label: 'Principals', to: ROUTES.accessPrincipals },
      { label: 'Sources', to: ROUTES.accessSources },
    ],
  },
  { label: 'Query', to: ROUTES.query, icon: Search },
]

/** Pages reached from the Settings popover rather than the main nav — listed
 *  here anyway so a page's header can still resolve a title via
 *  `findActiveLabel`, and the popover drives each row's destination from
 *  `to` instead of hardcoding it. */
export const SETTINGS_ITEMS: NavItem[] = [
  { label: 'Connections', to: ROUTES.connections, icon: Workflow },
]
