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
import { Plus } from 'lucide-react'
import { type FormEvent, useState } from 'react'
import { useFormReducer } from '@/shared/hooks/useFormReducer'
import { useCreatePrincipal } from '../hooks/usePrincipalMutations'
import type { PrincipalType, Role } from '../types/access.type'

interface FormState {
  principalId: string
  type: PrincipalType
  role: Role
}

const EMPTY: FormState = { principalId: '', type: 'SUBJECT', role: 'MEMBER' }

export function AddPrincipalDialog() {
  const [isOpen, setOpen] = useState<boolean>(false)
  const [form, setField, replace] = useFormReducer(EMPTY)
  const create = useCreatePrincipal()

  function close() {
    setOpen(false)
    replace(EMPTY)
    create.reset()
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    create.mutate(
      { principalId: form.principalId.trim(), type: form.type, role: form.role },
      { onSuccess: close },
    )
  }

  return (
    <>
      <Button size="sm" variant="secondary" onPress={() => setOpen(true)}>
        <Plus size={16} />
        Principal
      </Button>

      <Modal.Backdrop isOpen={isOpen} onOpenChange={setOpen}>
        <Modal.Container>
          <Modal.Dialog className="sm:max-w-105">
            <Modal.CloseTrigger />
            <Modal.Header>
              <Modal.Heading>Add a principal</Modal.Heading>
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
                  <Input placeholder="alice, eng-team" />
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

                <Select
                  placeholder="Select a role"
                  selectedKey={form.role}
                  onSelectionChange={(key) => setField('role')(key as Role)}
                  variant="secondary"
                >
                  <Label>Role</Label>
                  <Select.Trigger>
                    <Select.Value />
                    <Select.Indicator />
                  </Select.Trigger>
                  <Select.Popover>
                    <ListBox>
                      <ListBox.Item id="MEMBER" textValue="Member">
                        Member
                        <ListBox.ItemIndicator />
                      </ListBox.Item>
                      <ListBox.Item id="ADMIN" textValue="Admin">
                        Admin
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
                  Add
                </Button>
              </Modal.Footer>
            </Form>
          </Modal.Dialog>
        </Modal.Container>
      </Modal.Backdrop>
    </>
  )
}
