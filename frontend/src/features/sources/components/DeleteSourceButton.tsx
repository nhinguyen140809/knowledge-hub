import { AlertDialog, Button } from '@heroui/react'
import { Trash2 } from 'lucide-react'
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
    <AlertDialog>
      <Button isIconOnly size="sm" variant="danger" aria-label={`Delete ${name}`}>
        <Trash2 size={16} />
      </Button>
      <AlertDialog.Backdrop>
        <AlertDialog.Container>
          <AlertDialog.Dialog className="sm:max-w-[420px]">
            <AlertDialog.CloseTrigger />
            <AlertDialog.Header>
              <AlertDialog.Icon status="danger">
                <Trash2 className="size-5" />
              </AlertDialog.Icon>
              <AlertDialog.Heading>Delete this source?</AlertDialog.Heading>
            </AlertDialog.Header>
            <AlertDialog.Body>
              <p>
                <strong>{name}</strong> and everything indexed from it will be removed. Queries will
                stop returning its content. This cannot be undone.
              </p>
            </AlertDialog.Body>
            <AlertDialog.Footer>
              <Button slot="close" variant="tertiary">
                Cancel
              </Button>
              <Button
                slot="close"
                variant="danger"
                isPending={remove.isPending}
                onPress={() => remove.mutate(sourceId, { onSuccess: onDeleted })}
              >
                Delete source
              </Button>
            </AlertDialog.Footer>
          </AlertDialog.Dialog>
        </AlertDialog.Container>
      </AlertDialog.Backdrop>
    </AlertDialog>
  )
}
