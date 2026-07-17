import { ThemeProvider as NextThemesProvider } from 'next-themes'
import { type ReactNode } from 'react'

/**
 * Theme context for the app. HeroUI v3 ships no provider of its own; its theme
 * keys off the `.dark` class and `data-theme` attribute on <html>, so next-themes
 * (the approach the HeroUI docs recommend) is configured to set both, persist the
 * choice, and default to the OS preference.
 */
export function ThemeProvider({ children }: { children: ReactNode }) {
  return (
    <NextThemesProvider
      attribute={['class', 'data-theme']}
      defaultTheme="system"
      enableSystem
      disableTransitionOnChange
    >
      {children}
    </NextThemesProvider>
  )
}
