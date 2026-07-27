import { Button, Form, Label, ListBox, Modal, Select } from '@heroui/react'
import { Plus } from 'lucide-react'
import { type FormEvent, useState } from 'react'
import { useGrantSources } from '../../shared/hooks/useGrants'
import { useGrantPrincipalCandidates } from '../hooks/useGrantPrincipalCandidates'

/** Grants this source directly to a principal. Only principals without an
 *  existing direct grant are offered — access that is inherited or comes from
 *  the default policy can still be granted directly, which makes it survive
 *  leaving the group or a policy flip. Admins are never offered: their role
 *  already reads everything, so a new grant would be dead config. */
export function GrantPrincipalDialog({ sourceId }: { sourceId: string | null }) {
  const [isOpen, setOpen] = useState<boolean>(false)
  const [principalId, setPrincipalId] = useState<string | null>(null)
  const { candidates, isLoading, isError } = useGrantPrincipalCandidates(sourceId)
  const grant = useGrantSources()

  const placeholder = isLoading
    ? 'Loading principals...'
    : isError
      ? 'Failed to load principals'
      : candidates.length === 0
        ? 'No principals to grant'
        : 'Select a principal'

  function close() {
    setOpen(false)
    setPrincipalId(null)
    grant.reset()
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    if (!sourceId || !principalId) return
    grant.mutate({ principalId, sourceIds: [sourceId] }, { onSuccess: close })
  }

  return (
    <>
      <Button size="sm" variant="primary" isDisabled={!sourceId} onPress={() => setOpen(true)}>
        <Plus size={16} />
        Principal
      </Button>

      <Modal.Backdrop isOpen={isOpen} onOpenChange={(open) => (open ? setOpen(true) : close())}>
        <Modal.Container>
          <Modal.Dialog className="sm:max-w-105">
            <Modal.CloseTrigger />
            <Modal.Header>
              <Modal.Heading className="mb-2">Grant {sourceId} to a principal</Modal.Heading>
            </Modal.Header>
            <Form onSubmit={onSubmit} className="flex min-h-0 flex-1 flex-col">
              <Modal.Body>
                <Select
                  placeholder={placeholder}
                  selectedKey={principalId}
                  onSelectionChange={(key) => setPrincipalId(key as string | null)}
                  isDisabled={isLoading || candidates.length === 0}
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
                <Button type="submit" isPending={grant.isPending} isDisabled={!principalId}>
                  <Plus size={16} />
                  Grant
                </Button>
              </Modal.Footer>
            </Form>
          </Modal.Dialog>
        </Modal.Container>
      </Modal.Backdrop>
    </>
  )
}
