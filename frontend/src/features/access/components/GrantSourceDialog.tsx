import { Button, Form, Label, ListBox, Modal, Select } from '@heroui/react'
import { Plus } from 'lucide-react'
import { type FormEvent, useState } from 'react'
import { useGrantSources } from '../hooks/useGrants'
import { useGrantSourceCandidates } from '../hooks/useGrantSourceCandidates'
import { usePrincipalGraph } from '../hooks/usePrincipals'
import { canReceiveGrants } from '../lib/principal.rules'

/** Grants one source directly to the selected principal. Only sources without
 *  an existing direct grant are offered — access that is inherited or comes
 *  from the default policy can still be granted directly, which makes it
 *  survive leaving the group or a policy flip. Disabled for admins: their
 *  role already reads everything, so a new grant would be dead config (their
 *  pre-existing grants stay visible and revocable in the list). */
export function GrantSourceDialog({ principalId }: { principalId: string | null }) {
  const [isOpen, setOpen] = useState<boolean>(false)
  const [sourceId, setSourceId] = useState<string | null>(null)
  const { candidates, isLoading, isError } = useGrantSourceCandidates(principalId)
  const grant = useGrantSources()

  // Cached — the same query the tree renders from, so this is a free lookup.
  const graph = usePrincipalGraph()
  const principal = graph.data?.principals.find((p) => p.principalId === principalId)
  const grantable = principal ? canReceiveGrants(principal) : true

  const placeholder = isLoading
    ? 'Loading sources...'
    : isError
      ? 'Failed to load sources'
      : candidates.length === 0
        ? 'No sources to grant'
        : 'Select a source'

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
      <Button
        size="sm"
        variant="primary"
        isDisabled={!principalId || !grantable}
        onPress={() => setOpen(true)}
      >
        <Plus size={16} />
        Source
      </Button>

      <Modal.Backdrop isOpen={isOpen} onOpenChange={(open) => (open ? setOpen(true) : close())}>
        <Modal.Container>
          <Modal.Dialog className="sm:max-w-105">
            <Modal.CloseTrigger />
            <Modal.Header>
              <Modal.Heading className="mb-2">Grant a source to {principalId}</Modal.Heading>
            </Modal.Header>
            <Form onSubmit={onSubmit} className="flex min-h-0 flex-1 flex-col">
              <Modal.Body>
                <Select
                  placeholder={placeholder}
                  selectedKey={sourceId}
                  onSelectionChange={(key) => setSourceId(key as string | null)}
                  isDisabled={isLoading || candidates.length === 0}
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
