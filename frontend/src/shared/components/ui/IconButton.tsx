import { Button, Tooltip } from '@heroui/react'
import type { ComponentProps, ReactNode } from 'react'

/** Every Button prop except the two this component owns: it is always
 *  `isIconOnly`, and its `children` is the icon. */
interface IconButtonProps extends Omit<ComponentProps<typeof Button>, 'isIconOnly' | 'children'> {
  /** Doubles as the tooltip text and the accessible name — an icon-only button
   *  has no visible label, so both come from one word. */
  tooltip: string
  children: ReactNode
}

/** Icon-only button that always carries a tooltip. Pairing Button and Tooltip
 *  in one place keeps every icon action consistent and spares each call site
 *  from repeating the aria-label/tooltip pair. Works as a dialog or menu
 *  trigger: React Aria lets a tooltip and an overlay share one trigger button. */
export function IconButton({ tooltip, children, ...props }: IconButtonProps) {
  return (
    <Tooltip delay={300}>
      <Button isIconOnly aria-label={tooltip} {...props}>
        {children}
      </Button>
      <Tooltip.Content>{tooltip}</Tooltip.Content>
    </Tooltip>
  )
}
