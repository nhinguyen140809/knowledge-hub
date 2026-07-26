import { Button, Form, Label, ListBox, Modal, Select } from '@heroui/react'
import { FolderPlus } from 'lucide-react'
import { type FormEvent, useState } from 'react'
import { useAddMember } from '../../hooks/usePrincipalMutations'
import type { Principal } from '../../types/access.type'

interface AddToGroupDialogProps {
  target: Principal | null
  /** Eligible groups — the principal itself, groups already containing it,
   *  and its descendants (cycle guard) are already excluded by the caller. */
  candidates: Principal[]
  onOpenChange: (isOpen: boolean) => void
}

/** Adds one more membership for an existing principal — membership is a DAG,
 *  so this is the "also in that group" gesture: every current membership
 *  stays. Move-to-group is the exchanging counterpart (removes one edge,
 *  adds another). */
export function AddToGroupDialog({ target, candidates, onOpenChange }: AddToGroupDialogProps) {
  const [groupId, setGroupId] = useState<string | null>(null)
  const addMember = useAddMember()

  function close() {
    onOpenChange(false)
    setGroupId(null)
    addMember.reset()
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    if (!target || !groupId) return
    addMember.mutate({ groupId, memberId: target.principalId }, { onSuccess: close })
  }

  return (
    <Modal.Backdrop isOpen={target !== null} onOpenChange={(isOpen) => !isOpen && close()}>
      <Modal.Container>
        <Modal.Dialog className="sm:max-w-105">
          <Modal.CloseTrigger />
          <Modal.Header>
            <Modal.Heading>Add {target?.principalId} to a group</Modal.Heading>
          </Modal.Header>
          <Form onSubmit={onSubmit} className="flex min-h-0 flex-1 flex-col">
            <Modal.Body className="flex flex-col gap-3">
              <p className="text-muted text-xs">Its current memberships are kept</p>
              <Select
                placeholder={candidates.length === 0 ? 'No eligible groups' : 'Select a group'}
                selectedKey={groupId}
                onSelectionChange={(key) => setGroupId(key as string | null)}
                isDisabled={candidates.length === 0}
                variant="secondary"
              >
                <Label>Group</Label>
                <Select.Trigger>
                  <Select.Value />
                  <Select.Indicator />
                </Select.Trigger>
                <Select.Popover>
                  <ListBox>
                    {candidates.map((g) => (
                      <ListBox.Item
                        key={g.principalId}
                        id={g.principalId}
                        textValue={g.principalId}
                      >
                        {g.principalId}
                        <ListBox.ItemIndicator />
                      </ListBox.Item>
                    ))}
                  </ListBox>
                </Select.Popover>
              </Select>
            </Modal.Body>
            <Modal.Footer>
              <Button variant="tertiary" onPress={close}>
                Cancel
              </Button>
              <Button type="submit" isPending={addMember.isPending} isDisabled={!groupId}>
                <FolderPlus size={16} />
                Add
              </Button>
            </Modal.Footer>
          </Form>
        </Modal.Dialog>
      </Modal.Container>
    </Modal.Backdrop>
  )
}
