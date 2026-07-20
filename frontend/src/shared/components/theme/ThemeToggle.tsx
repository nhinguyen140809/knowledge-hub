import { Button } from '@heroui/react'
import { Moon, Sun } from 'lucide-react'
import { useTheme } from 'next-themes'

/** Flips between light and dark. `resolvedTheme` is undefined until next-themes
 *  reads the stored/system preference after mount, so the icon renders only once
 *  it is known — avoiding a wrong-icon flash without an extra mounted flag. */
export function ThemeToggle() {
  const { resolvedTheme, setTheme } = useTheme()
  const isDark = resolvedTheme === 'dark'
  return (
    <Button
      isIconOnly
      variant="tertiary"
      aria-label={isDark ? 'Switch to light theme' : 'Switch to dark theme'}
      onPress={() => setTheme(isDark ? 'light' : 'dark')}
    >
      {resolvedTheme ? (
        isDark ? (
          <Sun size={18} aria-hidden />
        ) : (
          <Moon size={18} aria-hidden />
        )
      ) : null}
    </Button>
  )
}
