import {
  Button,
  FieldError,
  Form,
  Input,
  Label,
  ListBox,
  Modal,
  Select,
  TextArea,
  TextField,
} from '@heroui/react'
import { Plus } from 'lucide-react'
import { type FormEvent, useState } from 'react'
import { TagField } from '@/shared/components/ui/TagField'
import { useFormReducer } from '@/shared/hooks/useFormReducer'
import { SOURCE_TYPE_LOCATION } from '../constants/source.config'
import { useCreateSource } from '../hooks/useSourceMutations'
import type { SourceType } from '../types/source.type'

/** Lowercase letters, digits and single hyphens between them — the id ends up
 *  in URL paths, and nothing beyond non-blank is required elsewhere, so this
 *  is enforced client-side. */
const ID_PATTERN = /^[a-z0-9]+(-[a-z0-9]+)*$/

function validateId(value: string): string | null {
  return ID_PATTERN.test(value)
    ? null
    : 'Lowercase letters, numbers and hyphens only (e.g. engineering-wiki)'
}

interface FormState {
  id: string
  type: SourceType
  uriOrPath: string
  ref: string
  name: string
  description: string
  include: string[]
  ignore: string[]
}

const EMPTY: FormState = {
  id: '',
  type: 'GIT',
  uriOrPath: '',
  ref: '',
  name: '',
  description: '',
  include: [],
  ignore: [],
}

export function CreateSourceDialog() {
  const [isOpen, setOpen] = useState<boolean>(false)
  const [form, setField, replace] = useFormReducer(EMPTY)
  const create = useCreateSource()
  const { hasRef, formLabel, formPlaceholder } = SOURCE_TYPE_LOCATION[form.type]

  function close() {
    setOpen(false)
    replace(EMPTY)
    create.reset()
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    create.mutate(
      {
        id: form.id.trim(),
        type: form.type,
        uriOrPath: form.uriOrPath.trim(),
        // The server treats an omitted field as "unset"; send null rather than ''.
        ref: hasRef && form.ref.trim() ? form.ref.trim() : null,
        include: form.include,
        ignore: form.ignore,
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
        <Modal.Container size="cover">
          <Modal.Dialog>
            <Modal.CloseTrigger />
            <Modal.Header>
              <Modal.Heading className="mb-2 text-lg font-bold">Register a source</Modal.Heading>
            </Modal.Header>
            <Form onSubmit={onSubmit} className="flex min-h-0 flex-1 flex-col">
              <Modal.Body className="grid gap-6 sm:grid-cols-2">
                <div className="flex flex-col gap-4">
                  <div className="grid gap-4 sm:grid-cols-2">
                    <TextField
                      value={form.id}
                      onChange={setField('id')}
                      isRequired
                      variant="secondary"
                      validate={validateId}
                    >
                      <Label>Id</Label>
                      <Input placeholder="engineering-wiki" />
                      <FieldError />
                    </TextField>

                    <Select
                      placeholder="Select a type"
                      selectedKey={form.type}
                      onSelectionChange={(key) => setField('type')(key as SourceType)}
                      variant="secondary"
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

                  <TextField
                    value={form.uriOrPath}
                    onChange={setField('uriOrPath')}
                    isRequired
                    variant="secondary"
                  >
                    <Label>{formLabel}</Label>
                    <Input placeholder={formPlaceholder} />
                  </TextField>

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
                </div>

                <div className="flex flex-col gap-4">
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
                </div>
              </Modal.Body>
              <Modal.Footer>
                <Button variant="tertiary" onPress={close}>
                  Cancel
                </Button>
                <Button type="submit" isPending={create.isPending}>
                  Register
                </Button>
              </Modal.Footer>
            </Form>
          </Modal.Dialog>
        </Modal.Container>
      </Modal.Backdrop>
    </>
  )
}
