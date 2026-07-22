import { Button } from '@heroui/react'
import { Trash2 } from 'lucide-react'
import { ConfirmDialog } from '@/shared/components/ui/ConfirmDialog'
import type { Principal } from '../../types/access.type'

interface DeletePrincipalDialogProps {
  target: Principal | null
  onOpenChange: (isOpen: boolean) => void
  isPending: boolean
  onConfirm: () => void
}

export function DeletePrincipalDialog({
  target,
  onOpenChange,
  isPending,
  onConfirm,
}: DeletePrincipalDialogProps) {
  return (
    <ConfirmDialog
      isOpen={target !== null}
      onOpenChange={onOpenChange}
      icon={<Trash2 className="size-5" />}
      heading="Delete this principal?"
      message={
        <p>
          <strong>{target?.principalId}</strong> is removed along with every membership and grant
          edge it has, including group membership, group members, and any sources granted directly
          to it. This cannot be undone.
        </p>
      }
      confirmButton={
        <Button slot="close" variant="danger" isPending={isPending} onPress={onConfirm}>
          Delete
        </Button>
      }
    />
  )
}
