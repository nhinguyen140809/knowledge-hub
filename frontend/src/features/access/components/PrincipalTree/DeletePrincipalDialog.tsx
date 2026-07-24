import { Button } from '@heroui/react'
import { Trash2 } from 'lucide-react'
import { ConfirmDialog } from '@/shared/components/ui/ConfirmDialog'
import { useDeletePrincipal } from '../../hooks/usePrincipalMutations'
import type { Principal } from '../../types/access.type'

interface DeletePrincipalDialogProps {
  target: Principal | null
  onOpenChange: (isOpen: boolean) => void
  /** Fires after the delete succeeds — the owner of the selection needs it to
   *  drop a selection that now points at nothing. */
  onDeleted?: (principalId: string) => void
}

export function DeletePrincipalDialog({
  target,
  onOpenChange,
  onDeleted,
}: DeletePrincipalDialogProps) {
  const deletePrincipal = useDeletePrincipal()

  function onConfirm() {
    if (!target) return
    // Captured before mutating: the dialog closes (clearing target) while the
    // request is still in flight.
    const id = target.principalId
    deletePrincipal.mutate(id, { onSuccess: () => onDeleted?.(id) })
  }

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
        <Button
          slot="close"
          variant="danger"
          isPending={deletePrincipal.isPending}
          onPress={onConfirm}
        >
          Delete
        </Button>
      }
    />
  )
}
