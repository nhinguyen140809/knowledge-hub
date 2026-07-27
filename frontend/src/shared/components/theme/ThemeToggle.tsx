import { Button } from '@heroui/react'
import { Moon, Sun } from 'lucide-react'
import { useTheme } from 'next-themes'

/** Flips between light and dark, rendered as a plain row (ghost, full-width,
 *  left-aligned) so it drops into a menu-like container — a sidebar footer, a
 *  popover — without pulling in that container's own row component.
 *  `resolvedTheme` is undefined until next-themes reads the stored/system
 *  preference after mount, so the icon renders only once it is known —
 *  avoiding a wrong-icon flash without an extra mounted flag. */
export function ThemeToggle() {
  const { resolvedTheme, setTheme } = useTheme()
  const isDark = resolvedTheme === 'dark'
  return (
    <Button
      fullWidth
      variant="ghost"
      className="justify-start font-normal"
      onPress={() => setTheme(isDark ? 'light' : 'dark')}
    >
      {resolvedTheme ? (
        isDark ? (
          <Sun size={16} aria-hidden />
        ) : (
          <Moon size={16} aria-hidden />
        )
      ) : null}
      {isDark ? 'Light mode' : 'Dark mode'}
    </Button>
  )
}
