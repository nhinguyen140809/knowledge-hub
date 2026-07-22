import { Button, Form, Label, ListBox, Modal, Select } from '@heroui/react'
import { UserPlus } from 'lucide-react'
import { type FormEvent, useState } from 'react'
import { useAddMember } from '../../hooks/usePrincipalMutations'
import type { Principal } from '../../types/access.type'

interface AddMemberDialogProps {
  group: Principal | null
  /** Principals eligible to join — the group itself and its current direct
   *  members are already excluded by the caller. */
  candidates: Principal[]
  onOpenChange: (isOpen: boolean) => void
}

/** Adding a member links two principals that already exist — it never creates
 *  one. That's the other half of "add principal": creating a principal and
 *  putting it in a group are deliberately separate actions. */
export function AddMemberDialog({ group, candidates, onOpenChange }: AddMemberDialogProps) {
  const [memberId, setMemberId] = useState<string | null>(null)
  const addMember = useAddMember()

  function close() {
    onOpenChange(false)
    setMemberId(null)
    addMember.reset()
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    if (!group || !memberId) return
    addMember.mutate({ groupId: group.principalId, memberId }, { onSuccess: close })
  }

  return (
    <Modal.Backdrop isOpen={group !== null} onOpenChange={(isOpen) => !isOpen && close()}>
      <Modal.Container>
        <Modal.Dialog className="sm:max-w-105">
          <Modal.CloseTrigger />
          <Modal.Header>
            <Modal.Heading>Add a member to {group?.principalId}</Modal.Heading>
          </Modal.Header>
          <Form onSubmit={onSubmit} className="flex min-h-0 flex-1 flex-col">
            <Modal.Body>
              <Select
                placeholder={
                  candidates.length === 0 ? 'No eligible principals' : 'Select a principal'
                }
                selectedKey={memberId}
                onSelectionChange={(key) => setMemberId(key as string | null)}
                isDisabled={candidates.length === 0}
                variant="secondary"
              >
                <Label>Principal</Label>
                <Select.Trigger>
                  <Select.Value />
                  <Select.Indicator />
                </Select.Trigger>
                <Select.Popover>
                  <ListBox>
                    {candidates.map((p) => (
                      <ListBox.Item
                        key={p.principalId}
                        id={p.principalId}
                        textValue={p.principalId}
                      >
                        {p.principalId}
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
              <Button type="submit" isPending={addMember.isPending} isDisabled={!memberId}>
                <UserPlus size={16} />
                Add
              </Button>
            </Modal.Footer>
          </Form>
        </Modal.Dialog>
      </Modal.Container>
    </Modal.Backdrop>
  )
}
