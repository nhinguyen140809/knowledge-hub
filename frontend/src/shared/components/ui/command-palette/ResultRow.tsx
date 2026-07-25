import { Button } from '@heroui/react'
import { CornerDownLeft } from 'lucide-react'
import { useEffect, useRef } from 'react'
import type { CommandItem } from './types'

interface ResultRowProps {
  item: CommandItem
  isActive: boolean
  onActivate: () => void
  onRun: () => void
}

/** One result row: icon, label, kind hint, and an enter glyph on the active row.
 *  Hovering activates it so mouse and keyboard share one highlight. A ghost,
 *  full-width Button so it reads as a flat row while keeping React Aria's press
 *  and focus behaviour. */
export function ResultRow({ item, isActive, onActivate, onRun }: ResultRowProps) {
  const Icon = item.icon
  const ref = useRef<HTMLButtonElement>(null)

  // Keep the keyboard-highlighted row in view as the selection moves past the
  // fold. 'nearest' leaves an already-visible row where it is, so hovering a
  // visible item doesn't cause a jump.
  useEffect(() => {
    if (isActive) ref.current?.scrollIntoView({ block: 'nearest' })
  }, [isActive])

  return (
    <Button
      ref={ref}
      variant="ghost"
      fullWidth
      onPress={onRun}
      onHoverStart={onActivate}
      className={`flex items-center justify-start gap-3 rounded-lg px-3 py-2 text-left text-sm ${
        isActive ? 'bg-surface-secondary' : ''
      }`}
    >
      {Icon && <Icon size={15} className="text-muted shrink-0" />}
      <span className="min-w-0 flex-1 truncate">{item.label}</span>
      {item.hint && <span className="text-muted text-xs">{item.hint}</span>}
      {isActive && <CornerDownLeft size={14} className="text-muted shrink-0" />}
    </Button>
  )
}
