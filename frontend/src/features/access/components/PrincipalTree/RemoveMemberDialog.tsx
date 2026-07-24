import { Button } from '@heroui/react'
import { UserMinus } from 'lucide-react'
import { ConfirmDialog } from '@/shared/components/ui/ConfirmDialog'
import { useRemoveMember } from '../../hooks/usePrincipalMutations'
import type { Principal } from '../../types/access.type'

export interface RemoveMemberTarget {
  groupId: string
  member: Principal
}

interface RemoveMemberDialogProps {
  target: RemoveMemberTarget | null
  onOpenChange: (isOpen: boolean) => void
}

/** Owns its mutation like the other tree dialogs do; ConfirmDialog itself
 *  stays a dumb shell, so this wrapper is where mutation becomes props. */
export function RemoveMemberDialog({ target, onOpenChange }: RemoveMemberDialogProps) {
  const removeMember = useRemoveMember()

  function onConfirm() {
    if (!target) return
    removeMember.mutate({ groupId: target.groupId, memberId: target.member.principalId })
  }

  return (
    <ConfirmDialog
      isOpen={target !== null}
      onOpenChange={onOpenChange}
      icon={<UserMinus className="size-5" />}
      heading="Remove from group?"
      message={
        <p>
          <strong>{target?.member.principalId}</strong> is removed from{' '}
          <strong>{target?.groupId}</strong>. Only this one membership edge is removed: it keeps its
          own grants and any access inherited through other groups.
        </p>
      }
      confirmButton={
        <Button
          slot="close"
          variant="danger"
          isPending={removeMember.isPending}
          onPress={onConfirm}
        >
          Remove
        </Button>
      }
    />
  )
}
