import type { NavItem, NavMatch } from '@/shared/types/navigation.type'

const escapeRegExp = (s: string) => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

/** Decides whether a nav entry pointing at `to` should highlight as active for
 *  the current `pathname`. 'prefix' matches subpages but only at segment
 *  boundaries — /sources claims /sources/abc-123, never /sources-old. */
export function isActivePath(pathname: string, to: string, match: NavMatch = 'prefix'): boolean {
  const base = escapeRegExp(to.replace(/\/+$/, '')) || '/'
  const pattern = match === 'exact' ? `^${base}/?$` : `^${base}(/.*)?$`
  return new RegExp(pattern).test(pathname)
}

/** Label of the nav entry (or group child) matching the pathname; '' if none. */
export function findActiveLabel(items: NavItem[], pathname: string): string {
  for (const item of items) {
    if (item.to && isActivePath(pathname, item.to, item.match)) return item.label
    const child = item.children?.find((c) => isActivePath(pathname, c.to, c.match))
    if (child) return child.label
  }
  return ''
}
