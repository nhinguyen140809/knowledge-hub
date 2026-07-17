import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { initTheme } from '@/lib/theme.init'

function resetRoot() {
  const root = document.documentElement
  root.className = ''
  delete root.dataset.theme
  root.style.colorScheme = ''
  localStorage.clear()
}

function stubSystemDark(matches: boolean) {
  vi.stubGlobal('matchMedia', vi.fn().mockReturnValue({ matches } as MediaQueryList))
}

describe('initTheme', () => {
  beforeEach(resetRoot)
  afterEach(() => vi.unstubAllGlobals())

  it('applies the persisted dark theme', () => {
    localStorage.setItem('theme', 'dark')
    stubSystemDark(false)

    initTheme()

    const root = document.documentElement
    expect(root.classList.contains('dark')).toBe(true)
    expect(root.dataset.theme).toBe('dark')
    expect(root.style.colorScheme).toBe('dark')
  })

  it('falls back to the system preference when nothing is persisted', () => {
    stubSystemDark(true)

    initTheme()

    expect(document.documentElement.classList.contains('dark')).toBe(true)
  })

  it('applies light when persisted theme is light even on a dark system', () => {
    localStorage.setItem('theme', 'light')
    stubSystemDark(true)

    initTheme()

    const root = document.documentElement
    expect(root.classList.contains('dark')).toBe(false)
    expect(root.dataset.theme).toBe('light')
  })
})
