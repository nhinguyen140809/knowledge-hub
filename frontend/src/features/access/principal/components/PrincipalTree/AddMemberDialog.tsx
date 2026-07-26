import {
  Button,
  FieldError,
  Form,
  Input,
  Label,
  ListBox,
  Modal,
  Select,
  TextField,
} from '@heroui/react'
import { UserPlus } from 'lucide-react'
import { type FormEvent } from 'react'
import { useFormReducer } from '@/shared/hooks/useFormReducer'
import { useCreatePrincipal } from '../../hooks/usePrincipalMutations'
import { ROLE_IN_GROUP } from '../../lib/principal.rules'
import type { Principal, PrincipalType } from '../../types/access.type'

interface AddMemberDialogProps {
  group: Principal | null
  onOpenChange: (isOpen: boolean) => void
}

interface FormState {
  principalId: string
  type: PrincipalType
}

const EMPTY: FormState = { principalId: '', type: 'SUBJECT' }

/** Creates a brand-new principal born directly inside the group, in one atomic
 *  step. Putting an *existing* principal into a group is Move-to-group's job.
 *  No role field: admins stay out of the membership graph, so a principal
 *  created inside a group is always a MEMBER. */
export function AddMemberDialog({ group, onOpenChange }: AddMemberDialogProps) {
  const [form, setField, replace] = useFormReducer(EMPTY)
  const create = useCreatePrincipal()

  function close() {
    onOpenChange(false)
    replace(EMPTY)
    create.reset()
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    if (!group) return
    create.mutate(
      {
        parentGroupId: group.principalId,
        principalId: form.principalId.trim(),
        type: form.type,
        role: ROLE_IN_GROUP,
      },
      { onSuccess: close },
    )
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
            <Modal.Body className="flex flex-col gap-4">
              <TextField
                value={form.principalId}
                onChange={setField('principalId')}
                isRequired
                variant="secondary"
              >
                <Label>Principal id</Label>
                <Input placeholder="dave, qa-team" />
                <FieldError />
              </TextField>

              <Select
                placeholder="Select a type"
                selectedKey={form.type}
                onSelectionChange={(key) => setField('type')(key as PrincipalType)}
                variant="secondary"
              >
                <Label>Type</Label>
                <Select.Trigger>
                  <Select.Value />
                  <Select.Indicator />
                </Select.Trigger>
                <Select.Popover>
                  <ListBox>
                    <ListBox.Item id="SUBJECT" textValue="Subject">
                      Subject
                      <ListBox.ItemIndicator />
                    </ListBox.Item>
                    <ListBox.Item id="GROUP" textValue="Group">
                      Group
                      <ListBox.ItemIndicator />
                    </ListBox.Item>
                  </ListBox>
                </Select.Popover>
              </Select>
            </Modal.Body>
            <Modal.Footer>
              <Button variant="tertiary" onPress={close}>
                Cancel
              </Button>
              <Button type="submit" isPending={create.isPending}>
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
