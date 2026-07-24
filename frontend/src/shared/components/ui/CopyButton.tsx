import { Check, Copy } from 'lucide-react'
import type { ComponentProps } from 'react'
import { IconButton } from '@/shared/components/ui/IconButton'
import { useCopyToClipboard } from '@/shared/hooks/useCopyToClipboard'

/** Everything an IconButton takes except the parts CopyButton owns: the tooltip
 *  (driven by copy state), the icon, and the press handler (always the copy). */
interface CopyButtonProps extends Omit<
  ComponentProps<typeof IconButton>,
  'tooltip' | 'children' | 'onPress'
> {
  /** The exact text written to the clipboard, which may differ from what the
   *  surrounding UI shows truncated (a full commit sha behind a short one). */
  value: string
  /** Names the value for the tooltip and assistive tech, e.g. "commit sha". */
  label: string
}

/** Inline icon button that copies `value` and briefly confirms with a check
 *  before returning to the copy affordance. Size/variant/className can be
 *  overridden per call site. */
export function CopyButton({ value, label, ...props }: CopyButtonProps) {
  const { copied, copy } = useCopyToClipboard()
  const text = copied ? 'Copied' : `Copy ${label}`
  return (
    <IconButton tooltip={text} size="sm" variant="ghost" {...props} onPress={() => copy(value)}>
      {copied ? <Check size={13} className="text-success" /> : <Copy size={13} />}
    </IconButton>
  )
}
