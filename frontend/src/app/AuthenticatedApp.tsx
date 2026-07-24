import { CommandPalette } from '@/shared/components/command-palette'
import { AppLayout } from '@/shared/components/layout/AppLayout'
import { useAppCommandItems } from './useAppCommandItems'

/** The signed-in shell: the page layout plus app-wide overlays that live
 *  outside any single route. The palette is a generic shared widget; the
 *  app-layer adapter here is what wires it to this product's principals and
 *  sources, keeping the layout and the palette free of each other's concerns. */
export function AuthenticatedApp() {
  return (
    <>
      <AppLayout />
      <CommandPalette useItems={useAppCommandItems} />
    </>
  )
}
