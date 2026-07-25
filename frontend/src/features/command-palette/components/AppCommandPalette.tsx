import { CommandPalette } from '@/shared/components/ui/command-palette'
import { useAppCommandItems } from '../hooks/useAppCommandItems'

/** The app's command palette: the generic palette wired to this product's
 *  principals and sources. Mount once inside the authenticated shell. */
export function AppCommandPalette() {
  return <CommandPalette useItems={useAppCommandItems} />
}
