import { Button } from '@heroui/react'
import { Trash2 } from 'lucide-react'
import { ConfirmDialog } from '@/shared/components/ui/ConfirmDialog'
import { useDeleteSource } from '../hooks/useSourceMutations'

interface DeleteSourceButtonProps {
  sourceId: string
  label?: string
  /** Called after the delete succeeds — e.g. to navigate away from a detail page. */
  onDeleted?: () => void
}

/** Deleting a source drops its whole index, so it always goes through an
 *  explicit confirmation naming the source. */
export function DeleteSourceButton({ sourceId, label, onDeleted }: DeleteSourceButtonProps) {
  const remove = useDeleteSource()
  const name = label ?? sourceId

  return (
    <ConfirmDialog
      trigger={
        <Button isIconOnly size="sm" variant="danger-soft" aria-label={`Delete ${name}`}>
          <Trash2 size={16} />
        </Button>
      }
      icon={<Trash2 className="size-5" />}
      heading="Delete this source?"
      message={
        <p>
          <strong>{name}</strong> and everything indexed from it will be removed. Queries will stop
          returning its content. This cannot be undone.
        </p>
      }
      confirmButton={
        <Button
          slot="close"
          variant="danger"
          isPending={remove.isPending}
          onPress={() => remove.mutate(sourceId, { onSuccess: onDeleted })}
        >
          Delete source
        </Button>
      }
    />
  )
}
