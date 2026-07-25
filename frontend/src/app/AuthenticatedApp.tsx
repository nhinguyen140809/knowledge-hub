import { AppCommandPalette } from '@/features/command-palette'
import { AppLayout } from '@/shared/components/layout/AppLayout'

/** The signed-in shell: the page layout plus app-wide overlays that live outside
 *  any single route. The command palette is its own feature (generic widget +
 *  app data), mounted here so it is reachable from every screen. */
export function AuthenticatedApp() {
  return (
    <>
      <AppLayout />
      <AppCommandPalette />
    </>
  )
}
