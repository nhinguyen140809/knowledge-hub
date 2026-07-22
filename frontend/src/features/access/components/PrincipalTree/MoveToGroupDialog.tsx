import { Button, Form, Label, ListBox, Modal, Select } from '@heroui/react'
import { FolderInput } from 'lucide-react'
import { type FormEvent, useState } from 'react'
import { useAddMember, useRemoveMember } from '../../hooks/usePrincipalMutations'
import type { Principal } from '../../types/access.type'

export interface MoveToGroupTarget {
  principal: Principal
  /** The group this principal is nested under in the row that was
   *  right-clicked, if any — undefined at the top level, where there's
   *  nothing to remove membership from first. */
  fromGroupId?: string
}

interface MoveToGroupDialogProps {
  target: MoveToGroupTarget | null
  /** Eligible target groups — the principal itself and its current parent
   *  (if any) are already excluded by the caller. */
  candidates: Principal[]
  onOpenChange: (isOpen: boolean) => void
}

/** Moving is two edge operations, not a dedicated backend primitive. Add runs
 *  first: if it fails, the old membership is untouched, whereas removing first
 *  could drop the principal from both groups on a partial failure. The
 *  in-between state (briefly in both groups) grants at most extra read access
 *  for a moment, which is the safer direction. If the principal has no current
 *  parent in this row, this degrades to a plain add. */
export function MoveToGroupDialog({ target, candidates, onOpenChange }: MoveToGroupDialogProps) {
  const [groupId, setGroupId] = useState<string | null>(null)
  const addMember = useAddMember()
  const removeMember = useRemoveMember()
  const isPending = addMember.isPending || removeMember.isPending

  function close() {
    onOpenChange(false)
    setGroupId(null)
    addMember.reset()
    removeMember.reset()
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    if (!target || !groupId) return
    const memberId = target.principal.principalId
    try {
      await addMember.mutateAsync({ groupId, memberId })
      if (target.fromGroupId) {
        await removeMember.mutateAsync({ groupId: target.fromGroupId, memberId })
      }
      close()
    } catch {
      // Already toasted by the global mutation cache handler; swallowing here
      // only prevents an unhandled rejection. The dialog stays open for retry.
    }
  }

  return (
    <Modal.Backdrop isOpen={target !== null} onOpenChange={(isOpen) => !isOpen && close()}>
      <Modal.Container>
        <Modal.Dialog className="sm:max-w-105">
          <Modal.CloseTrigger />
          <Modal.Header>
            <Modal.Heading>Move {target?.principal.principalId} to a group</Modal.Heading>
          </Modal.Header>
          <Form onSubmit={onSubmit} className="flex min-h-0 flex-1 flex-col">
            <Modal.Body className="flex flex-col gap-3">
              {target?.fromGroupId && (
                <p className="text-muted text-xs">
                  Also removes it from <strong>{target.fromGroupId}</strong>.
                </p>
              )}
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
              <Button type="submit" isPending={isPending} isDisabled={!groupId}>
                <FolderInput size={16} />
                Move
              </Button>
            </Modal.Footer>
          </Form>
        </Modal.Dialog>
      </Modal.Container>
    </Modal.Backdrop>
  )
}
