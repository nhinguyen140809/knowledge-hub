import { Button, Input, Label, ListBox, Modal, Select, TextArea, TextField } from '@heroui/react'
import { Plus } from 'lucide-react'
import { type FormEvent, useState } from 'react'
import { useCreateSource } from '../hooks/useSourceMutations'
import type { SourceType } from '../types/source.type'

/** Globs are entered as a comma-separated line; empty entries are dropped so a
 *  trailing comma does not become a pattern that matches nothing. */
function parseGlobs(value: string): string[] {
  return value
    .split(',')
    .map((g) => g.trim())
    .filter(Boolean)
}

const EMPTY = { id: '', uriOrPath: '', ref: '', name: '', description: '', include: '', ignore: '' }

export function CreateSourceDialog() {
  const [isOpen, setOpen] = useState(false)
  const [type, setType] = useState<SourceType>('GIT')
  const [form, setForm] = useState(EMPTY)
  const create = useCreateSource()

  const set = (key: keyof typeof EMPTY) => (value: string) =>
    setForm((f) => ({ ...f, [key]: value }))

  function close() {
    setOpen(false)
    setForm(EMPTY)
    setType('GIT')
    create.reset()
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    create.mutate(
      {
        id: form.id.trim(),
        type,
        uriOrPath: form.uriOrPath.trim(),
        // The server treats an omitted field as "unset"; send null rather than ''.
        ref: type === 'GIT' && form.ref.trim() ? form.ref.trim() : null,
        include: parseGlobs(form.include),
        ignore: parseGlobs(form.ignore),
        name: form.name.trim() || null,
        description: form.description.trim() || null,
      },
      { onSuccess: close },
    )
  }

  return (
    <>
      <Button size="sm" variant="secondary" onPress={() => setOpen(true)}>
        <Plus size={16} />
        Source
      </Button>

      <Modal.Backdrop isOpen={isOpen} onOpenChange={setOpen}>
        <Modal.Container size="lg">
          <Modal.Dialog>
            <Modal.CloseTrigger />
            <Modal.Header>
              <Modal.Heading>Register a source</Modal.Heading>
            </Modal.Header>
            <form onSubmit={onSubmit}>
              <Modal.Body className="flex flex-col gap-4">
                <div className="grid gap-4 sm:grid-cols-2">
                  <TextField value={form.id} onChange={set('id')} isRequired>
                    <Label>Id</Label>
                    <Input placeholder="engineering-wiki" />
                  </TextField>

                  <Select
                    placeholder="Select a type"
                    selectedKey={type}
                    onSelectionChange={(key) => setType(key as SourceType)}
                  >
                    <Label>Type</Label>
                    <Select.Trigger>
                      <Select.Value />
                      <Select.Indicator />
                    </Select.Trigger>
                    <Select.Popover>
                      <ListBox>
                        <ListBox.Item id="GIT" textValue="Git repository">
                          Git repository
                          <ListBox.ItemIndicator />
                        </ListBox.Item>
                        <ListBox.Item id="FS" textValue="Filesystem folder">
                          Filesystem folder
                          <ListBox.ItemIndicator />
                        </ListBox.Item>
                      </ListBox>
                    </Select.Popover>
                  </Select>
                </div>

                <TextField value={form.uriOrPath} onChange={set('uriOrPath')} isRequired>
                  <Label>{type === 'GIT' ? 'Repository URL' : 'Folder path'}</Label>
                  <Input
                    placeholder={
                      type === 'GIT' ? 'https://github.com/acme/wiki.git' : '/srv/knowledge/docs'
                    }
                  />
                </TextField>

                {type === 'GIT' && (
                  <TextField value={form.ref} onChange={set('ref')}>
                    <Label>Ref (optional)</Label>
                    <Input placeholder="main" />
                  </TextField>
                )}

                <TextField value={form.name} onChange={set('name')}>
                  <Label>Name (optional)</Label>
                  <Input placeholder="Engineering Wiki" />
                </TextField>

                <TextField value={form.description} onChange={set('description')}>
                  <Label>Description (optional)</Label>
                  <TextArea placeholder="What this source contains" rows={2} />
                </TextField>

                <div className="grid gap-4 sm:grid-cols-2">
                  <TextField value={form.include} onChange={set('include')}>
                    <Label>Include globs</Label>
                    <Input placeholder="**/*.md, docs/**" />
                  </TextField>
                  <TextField value={form.ignore} onChange={set('ignore')}>
                    <Label>Ignore globs</Label>
                    <Input placeholder="archive/**" />
                  </TextField>
                </div>

                {create.isError && (
                  <p className="text-danger text-sm">{(create.error as Error).message}</p>
                )}
              </Modal.Body>
              <Modal.Footer>
                <Button variant="tertiary" onPress={close}>
                  Cancel
                </Button>
                <Button type="submit" isPending={create.isPending}>
                  Register
                </Button>
              </Modal.Footer>
            </form>
          </Modal.Dialog>
        </Modal.Container>
      </Modal.Backdrop>
    </>
  )
}
