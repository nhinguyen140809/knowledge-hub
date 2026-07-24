import { Button, Form, Input, Label, Modal, TextArea, TextField } from '@heroui/react'
import { Pencil } from 'lucide-react'
import { type FormEvent, useState } from 'react'
import { TagField } from '@/shared/components/ui/TagField'
import { useFormReducer } from '@/shared/hooks/useFormReducer'
import { SOURCE_TYPE_LOCATION } from '../constants/source.config'
import { useUpdateSource } from '../hooks/useSourceMutations'
import type { Source } from '../types/source.type'

interface FormState {
  ref: string
  name: string
  description: string
  include: string[]
  ignore: string[]
}

function toForm(source: Source): FormState {
  return {
    ref: source.ref ?? '',
    name: source.name ?? '',
    description: source.description ?? '',
    include: source.include,
    ignore: source.ignore,
  }
}

/** Edits a source's ref, name, description and glob patterns — id, type and
 *  location are fixed server-side and not shown here. Glob changes only take
 *  effect on the next sync (see SourceIndexCard). */
export function EditSourceDialog({ source }: { source: Source }) {
  const [isOpen, setOpen] = useState<boolean>(false)
  const [form, setField, replace] = useFormReducer(() => toForm(source))
  const update = useUpdateSource()
  const { hasRef } = SOURCE_TYPE_LOCATION[source.type]

  function open() {
    replace(toForm(source))
    setOpen(true)
  }

  function close() {
    setOpen(false)
    update.reset()
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    update.mutate(
      {
        id: source.id,
        input: {
          ref: hasRef ? form.ref.trim() || null : undefined,
          include: form.include,
          ignore: form.ignore,
          name: form.name.trim() || null,
          description: form.description.trim() || null,
        },
      },
      { onSuccess: close },
    )
  }

  return (
    <>
      <Button size="sm" variant="primary" onPress={open}>
        <Pencil size={16} />
        Edit
      </Button>

      <Modal.Backdrop isOpen={isOpen} onOpenChange={setOpen}>
        <Modal.Container size="lg">
          <Modal.Dialog>
            <Modal.CloseTrigger />
            <Modal.Header>
              <Modal.Heading className="mb-2 text-lg font-bold">
                Edit {source.name ?? source.id}
              </Modal.Heading>
            </Modal.Header>
            <Form onSubmit={onSubmit} className="flex min-h-0 flex-1 flex-col">
              <Modal.Body className="flex flex-col gap-4">
                {hasRef && (
                  <TextField value={form.ref} onChange={setField('ref')} variant="secondary">
                    <Label>Ref (optional)</Label>
                    <Input placeholder="main" />
                  </TextField>
                )}

                <TextField value={form.name} onChange={setField('name')} variant="secondary">
                  <Label>Name (optional)</Label>
                  <Input placeholder="Engineering Wiki" />
                </TextField>

                <TextField
                  value={form.description}
                  onChange={setField('description')}
                  variant="secondary"
                >
                  <Label>Description (optional)</Label>
                  <TextArea placeholder="What this source contains" rows={2} />
                </TextField>

                <TagField
                  label="Include globs"
                  value={form.include}
                  onChange={setField('include')}
                  placeholder="**/*.md, docs/**"
                />
                <TagField
                  label="Ignore globs"
                  value={form.ignore}
                  onChange={setField('ignore')}
                  placeholder="archive/**"
                />
              </Modal.Body>
              <Modal.Footer>
                <Button variant="tertiary" onPress={close}>
                  Cancel
                </Button>
                <Button type="submit" isPending={update.isPending}>
                  Save changes
                </Button>
              </Modal.Footer>
            </Form>
          </Modal.Dialog>
        </Modal.Container>
      </Modal.Backdrop>
    </>
  )
}
