import { Button, Form, Input, Label, Modal, TextField } from '@heroui/react'
import { Pencil } from 'lucide-react'
import { type FormEvent, useState } from 'react'
import { IconButton } from '@/shared/components/ui/IconButton'
import { useSetProductName } from '../hooks/useSetProductName'

/** Renames the product shown in the runtime panel (and used as the fallback
 *  connection label). The rename is admin-only on the server; a non-admin key gets
 *  a permission error surfaced by the global mutation toast. */
export function EditProductNameDialog({ current }: { current: string }) {
  const [isOpen, setOpen] = useState<boolean>(false)
  const [name, setName] = useState<string>(current)
  const rename = useSetProductName()

  function close() {
    setOpen(false)
    setName(current)
    rename.reset()
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    rename.mutate(name.trim(), { onSuccess: close })
  }

  return (
    <>
      <IconButton tooltip="Rename product" size="sm" variant="ghost" onPress={() => setOpen(true)}>
        <Pencil size={14} />
      </IconButton>

      <Modal.Backdrop isOpen={isOpen} onOpenChange={(open) => (open ? setOpen(true) : close())}>
        <Modal.Container>
          <Modal.Dialog className="sm:max-w-105">
            <Modal.CloseTrigger />
            <Modal.Header>
              <Modal.Heading className="mb-2">Rename product</Modal.Heading>
            </Modal.Header>
            <Form onSubmit={onSubmit} className="flex min-h-0 flex-1 flex-col">
              <Modal.Body>
                <TextField value={name} onChange={setName} isRequired autoFocus variant="secondary">
                  <Label>Product name</Label>
                  <Input placeholder="Knowledge Hub" />
                </TextField>
              </Modal.Body>
              <Modal.Footer>
                <Button variant="tertiary" onPress={close}>
                  Cancel
                </Button>
                <Button type="submit" isPending={rename.isPending} isDisabled={!name.trim()}>
                  Save
                </Button>
              </Modal.Footer>
            </Form>
          </Modal.Dialog>
        </Modal.Container>
      </Modal.Backdrop>
    </>
  )
}
