import { Button, Form, Label, ListBox, Modal, Select } from '@heroui/react'
import { Plus } from 'lucide-react'
import { type FormEvent, useState } from 'react'
import { useSources } from '@/features/sources'
import { useDirectGrants, useGrantSources } from '../hooks/useGrants'

/** Grants one source directly to the selected principal. Only sources without
 *  an existing direct grant are offered — access that is inherited or comes
 *  from the default policy can still be granted directly, which makes it
 *  survive leaving the group or a policy flip. */
export function GrantSourceDialog({ principalId }: { principalId: string | null }) {
  const [isOpen, setOpen] = useState<boolean>(false)
  const [sourceId, setSourceId] = useState<string | null>(null)
  const sources = useSources()
  const direct = useDirectGrants(principalId ?? undefined)
  const grant = useGrantSources()

  const granted = new Set(direct.data ?? [])
  const candidates = (sources.data ?? []).filter((s) => !granted.has(s.id))

  function close() {
    setOpen(false)
    setSourceId(null)
    grant.reset()
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    if (!principalId || !sourceId) return
    grant.mutate({ principalId, sourceIds: [sourceId] }, { onSuccess: close })
  }

  return (
    <>
      <Button size="sm" variant="primary" isDisabled={!principalId} onPress={() => setOpen(true)}>
        <Plus size={16} />
        Source
      </Button>

      <Modal.Backdrop isOpen={isOpen} onOpenChange={(open) => (open ? setOpen(true) : close())}>
        <Modal.Container>
          <Modal.Dialog className="sm:max-w-105">
            <Modal.CloseTrigger />
            <Modal.Header>
              <Modal.Heading>Grant a source to {principalId}</Modal.Heading>
            </Modal.Header>
            <Form onSubmit={onSubmit} className="flex min-h-0 flex-1 flex-col">
              <Modal.Body>
                <Select
                  placeholder={candidates.length === 0 ? 'No sources to grant' : 'Select a source'}
                  selectedKey={sourceId}
                  onSelectionChange={(key) => setSourceId(key as string | null)}
                  isDisabled={candidates.length === 0}
                  variant="secondary"
                >
                  <Label>Source</Label>
                  <Select.Trigger>
                    <Select.Value />
                    <Select.Indicator />
                  </Select.Trigger>
                  <Select.Popover>
                    <ListBox>
                      {candidates.map((s) => (
                        <ListBox.Item key={s.id} id={s.id} textValue={s.id}>
                          {s.id}
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
                <Button type="submit" isPending={grant.isPending} isDisabled={!sourceId}>
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
