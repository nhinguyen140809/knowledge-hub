import { Modal } from '@heroui/react'
import { useEffect, useState } from 'react'
import { CommandPaletteContent } from './CommandPaletteContent'
import type { CommandItem } from './types'

interface CommandPaletteProps {
  /** Produces the searchable items. Passed as a hook rather than an array so it
   *  runs only inside the content, which mounts only while open — nothing is
   *  fetched for a palette the user never opens. */
  useItems: () => CommandItem[]
}

/**
 * App-wide quick switcher. Cmd/Ctrl+K opens it anywhere; typing filters the
 * supplied items and selecting one navigates to its route. Domain-agnostic — it
 * knows nothing about what the items represent (see {@link CommandItem}).
 */
export function CommandPalette({ useItems }: CommandPaletteProps) {
  const [isOpen, setOpen] = useState(false)

  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault()
        setOpen((open) => !open)
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [])

  return (
    <Modal.Backdrop isOpen={isOpen} onOpenChange={setOpen}>
      <Modal.Container>
        <Modal.Dialog className="sm:max-w-150">
          {isOpen && <CommandPaletteContent useItems={useItems} onClose={() => setOpen(false)} />}
        </Modal.Dialog>
      </Modal.Container>
    </Modal.Backdrop>
  )
}
