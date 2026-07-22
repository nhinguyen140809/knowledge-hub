import { Button } from '@heroui/react'
import { UserMinus } from 'lucide-react'
import { ConfirmDialog } from '@/shared/components/ui/ConfirmDialog'
import type { Principal } from '../../types/access.type'

export interface RemoveMemberTarget {
  groupId: string
  member: Principal
}

interface RemoveMemberDialogProps {
  target: RemoveMemberTarget | null
  onOpenChange: (isOpen: boolean) => void
  isPending: boolean
  onConfirm: () => void
}

export function RemoveMemberDialog({
  target,
  onOpenChange,
  isPending,
  onConfirm,
}: RemoveMemberDialogProps) {
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
        <Button slot="close" variant="danger" isPending={isPending} onPress={onConfirm}>
          Remove
        </Button>
      }
    />
  )
}
