import { Moon, Sun } from 'lucide-react'
import { useTheme } from 'next-themes'
import { SidebarMenuButton } from '@/shared/components/ui/Sidebar'

/** Flips between light and dark, rendered as a sidebar row. `resolvedTheme` is
 *  undefined until next-themes reads the stored/system preference after mount,
 *  so the icon renders only once it is known — avoiding a wrong-icon flash
 *  without an extra mounted flag. */
export function ThemeToggle() {
  const { resolvedTheme, setTheme } = useTheme()
  const isDark = resolvedTheme === 'dark'
  return (
    <SidebarMenuButton onPress={() => setTheme(isDark ? 'light' : 'dark')}>
      {resolvedTheme ? (
        isDark ? (
          <Sun size={16} aria-hidden />
        ) : (
          <Moon size={16} aria-hidden />
        )
      ) : null}
      {isDark ? 'Light mode' : 'Dark mode'}
    </SidebarMenuButton>
  )
}
