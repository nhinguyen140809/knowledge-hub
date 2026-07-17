import type { LucideIcon } from 'lucide-react'

/** How a nav entry claims the current pathname:
 *  'exact'  — only the page itself (trailing slash tolerated);
 *  'prefix' — the page and any subpage, cut at segment boundaries. */
export type NavMatch = 'exact' | 'prefix'

export interface NavChild {
  label: string
  to: string
  match?: NavMatch
}

interface NavLeaf {
  label: string
  icon: LucideIcon
  to: string
  match?: NavMatch
  children?: undefined
}

interface NavGroup {
  label: string
  icon: LucideIcon
  children: NavChild[]
  to?: undefined
  match?: undefined
}

/** `children` present ⇒ the item renders as a collapsible group with a chevron. */
export type NavItem = NavLeaf | NavGroup
