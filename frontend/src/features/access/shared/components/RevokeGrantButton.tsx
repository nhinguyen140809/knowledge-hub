import { Button } from '@heroui/react'
import { CircleMinus } from 'lucide-react'
import { ConfirmDialog } from '@/shared/components/ui/ConfirmDialog'
import { IconButton } from '@/shared/components/ui/IconButton'
import { useRevokeSources } from '../hooks/useGrants'

/** Revoking a direct grant removes read access immediately, so it goes
 *  through the same confirm step as deleting a source or revoking a
 *  credential. Shared between the principal-centric and source-centric
 *  access panels — both revoke the same (principalId, sourceId) grant. */
export function RevokeGrantButton({
  principalId,
  sourceId,
}: {
  principalId: string
  sourceId: string
}) {
  const revoke = useRevokeSources()
  return (
    <ConfirmDialog
      trigger={
        <IconButton tooltip="Revoke access" size="sm" variant="ghost">
          <CircleMinus size={14} />
        </IconButton>
      }
      icon={<CircleMinus className="size-5" />}
      heading="Revoke this grant?"
      message={
        <p>
          <strong>{principalId}</strong> loses direct read access to <strong>{sourceId}</strong>{' '}
          immediately. It may still be reachable through a group grant or the default policy.
        </p>
      }
      confirmButton={
        <Button
          slot="close"
          variant="danger"
          isPending={revoke.isPending}
          onPress={() => revoke.mutate({ principalId, sourceIds: [sourceId] })}
        >
          Revoke
        </Button>
      }
    />
  )
}
