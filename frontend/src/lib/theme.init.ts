/**
 * Anti-FOUC: applies the persisted/system theme to <html> before first paint.
 * The inline-theme-init plugin (vite.config.ts) injects this file into <head>
 * as a blocking script, so keep it annotation-free and dependency-free.
 * next-themes takes over on mount using the same storage key ('theme') and
 * the same class + data-theme attributes, so there is no visible flip.
 */
export function initTheme() {
  try {
    const stored = localStorage.getItem('theme')
    const dark =
      stored === 'dark' ||
      ((!stored || stored === 'system') &&
        window.matchMedia('(prefers-color-scheme: dark)').matches)
    const theme = dark ? 'dark' : 'light'
    const root = document.documentElement
    root.classList.add(theme)
    root.dataset.theme = theme
    root.style.colorScheme = theme
  } catch {
    /* no storage access — leave the default light theme */
  }
}
