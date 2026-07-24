import { Button, Tooltip } from '@heroui/react'
import { RefreshCw } from 'lucide-react'
import { useSyncSource } from '../hooks/useSourceMutations'
import type { SyncResult } from '../types/source.type'

interface SyncSourceButtonProps {
  sourceId: string
  label?: string
  isIconOnly?: boolean
  /** Called with the result once the sync completes — e.g. to show its stats. */
  onSynced?: (result: SyncResult) => void
}

/** Triggers an incremental sync for one source. Sync is idempotent
 *  server-side, so re-running it is always safe. */
export function SyncSourceButton({ sourceId, label, isIconOnly, onSynced }: SyncSourceButtonProps) {
  const sync = useSyncSource()
  const name = label ?? sourceId

  const button = (
    <Button
      size="sm"
      variant="primary"
      isIconOnly={isIconOnly}
      isPending={sync.isPending}
      aria-label={isIconOnly ? `Sync ${name}` : undefined}
      onPress={() => sync.mutate(sourceId, { onSuccess: onSynced })}
    >
      <RefreshCw size={16} className={sync.isPending ? 'animate-spin' : ''} />
      {!isIconOnly && 'Sync'}
    </Button>
  )

  // Labelled, the button already reads "Sync"; only the icon-only form needs a
  // tooltip to name what it does (and which source).
  if (!isIconOnly) return button
  return (
    <Tooltip delay={300}>
      {button}
      <Tooltip.Content>{`Sync source`}</Tooltip.Content>
    </Tooltip>
  )
}
