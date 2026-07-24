import { Circle } from 'lucide-react'
import { describe, expect, it } from 'vitest'
import { findActiveLabel, isActivePath } from '@/shared/lib/navigation.utils'
import type { NavItem } from '@/shared/types/navigation.type'

describe('isActivePath', () => {
  it('exact: matches only the page itself, trailing slash tolerated', () => {
    expect(isActivePath('/', '/', 'exact')).toBe(true)
    expect(isActivePath('/sources', '/', 'exact')).toBe(false)
    expect(isActivePath('/sources/', '/sources', 'exact')).toBe(true)
    expect(isActivePath('/sources/abc', '/sources', 'exact')).toBe(false)
  })

  it('prefix: matches subpages, cut at segment boundaries', () => {
    expect(isActivePath('/sources', '/sources')).toBe(true)
    expect(isActivePath('/sources/abc-123', '/sources')).toBe(true)
    expect(isActivePath('/sources-old', '/sources')).toBe(false)
  })

  it('root as prefix never swallows other routes', () => {
    expect(isActivePath('/sources', '/')).toBe(false)
    expect(isActivePath('/', '/')).toBe(true)
  })
})

describe('findActiveLabel', () => {
  const items: NavItem[] = [
    { label: 'Dashboard', to: '/', icon: Circle, match: 'exact' },
    { label: 'Sources', to: '/sources', icon: Circle },
    {
      label: 'Access',
      icon: Circle,
      children: [{ label: 'Principals', to: '/access/principals' }],
    },
  ]

  it('resolves nav labels, including group children and subpages', () => {
    expect(findActiveLabel(items, '/')).toBe('Dashboard')
    expect(findActiveLabel(items, '/sources/abc-123')).toBe('Sources')
    expect(findActiveLabel(items, '/access/principals')).toBe('Principals')
  })

  it('returns an empty string for unknown paths', () => {
    expect(findActiveLabel(items, '/nope')).toBe('')
  })
})
