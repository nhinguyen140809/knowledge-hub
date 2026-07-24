import { Button, Form, Label, ListBox, Modal, Select } from '@heroui/react'
import { FolderInput } from 'lucide-react'
import { type FormEvent, useState } from 'react'
import { useMovePrincipal } from '../../hooks/usePrincipalMutations'
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
  /** Eligible target groups — the principal itself, every group already
   *  directly containing it, and its descendants (cycle guard) are already
   *  excluded by the caller. */
  candidates: Principal[]
  onOpenChange: (isOpen: boolean) => void
}

export function MoveToGroupDialog({ target, candidates, onOpenChange }: MoveToGroupDialogProps) {
  const [groupId, setGroupId] = useState<string | null>(null)
  const move = useMovePrincipal()
  const isPending = move.isPending

  function close() {
    onOpenChange(false)
    setGroupId(null)
    move.reset()
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    if (!target || !groupId) return
    move.mutate(
      {
        memberId: target.principal.principalId,
        fromGroupId: target.fromGroupId ?? null,
        toGroupId: groupId,
      },
      { onSuccess: close },
    )
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
