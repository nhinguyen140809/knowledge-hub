import { Button, Form, Input, Label, ListBox, NumberField, Select, TextField } from '@heroui/react'
import { Search } from 'lucide-react'
import type { FormEvent } from 'react'
import { useSources } from '@/features/sources'
import { useFormReducer } from '@/shared/hooks/useFormReducer'
import type { HitType, QueryInput } from '../types/query.type'

const TYPES: HitType[] = ['code', 'doc', 'requirement', 'commit']

interface FormState {
  text: string
  sourceId: string | 'any'
  ref: string
  type: HitType | 'any'
  topK: number | undefined
}

const EMPTY: FormState = { text: '', sourceId: 'any', ref: '', type: 'any', topK: undefined }

/** The search box plus the knobs the API accepts. All knobs *narrow* the search
 *  inside what the caller may already read — none of them can widen access. */
export function QueryForm({ onSubmit }: { onSubmit: (input: QueryInput) => void }) {
  const [form, setField] = useFormReducer(EMPTY)
  const sources = useSources()

  function submit(e: FormEvent) {
    e.preventDefault()
    if (!form.text.trim()) return
    onSubmit({
      text: form.text.trim(),
      topK: form.topK ?? null,
      sourceId: form.sourceId === 'any' ? null : form.sourceId,
      ref: form.ref.trim() || null,
      type: form.type === 'any' ? null : form.type,
    })
  }

  return (
    <Form onSubmit={submit} className="flex flex-col gap-4">
      <div className="flex items-end gap-2">
        <TextField value={form.text} onChange={setField('text')} className="flex-1" isRequired>
          <Label>Query</Label>
          <Input placeholder="How is the retrieval cache keyed?" />
        </TextField>
      </div>
      <div className="flex flex-row items-end justify-between gap-4">
        <div className="grid w-full gap-3 sm:grid-cols-4">
          <Select
            placeholder="Any source"
            selectedKey={form.sourceId}
            onSelectionChange={(key) => setField('sourceId')(key as string)}
          >
            <Label>Source</Label>
            <Select.Trigger>
              <Select.Value />
              <Select.Indicator />
            </Select.Trigger>
            <Select.Popover>
              <ListBox>
                <ListBox.Item id="any" textValue="Any source">
                  Any source
                  <ListBox.ItemIndicator />
                </ListBox.Item>
                {sources.data?.map((source) => (
                  <ListBox.Item key={source.id} id={source.id} textValue={source.name ?? source.id}>
                    {source.name ?? source.id}
                    <ListBox.ItemIndicator />
                  </ListBox.Item>
                ))}
              </ListBox>
            </Select.Popover>
          </Select>

          <TextField value={form.ref} onChange={setField('ref')}>
            <Label>Ref</Label>
            <Input placeholder="canonical" />
          </TextField>

          <Select
            placeholder="Any type"
            selectedKey={form.type}
            onSelectionChange={(key) => setField('type')(key as HitType | 'any')}
          >
            <Label>Type</Label>
            <Select.Trigger>
              <Select.Value />
              <Select.Indicator />
            </Select.Trigger>
            <Select.Popover>
              <ListBox>
                <ListBox.Item id="any" textValue="Any type">
                  Any type
                  <ListBox.ItemIndicator />
                </ListBox.Item>
                {TYPES.map((t) => (
                  <ListBox.Item key={t} id={t} textValue={t}>
                    {t}
                    <ListBox.ItemIndicator />
                  </ListBox.Item>
                ))}
              </ListBox>
            </Select.Popover>
          </Select>

          <NumberField value={form.topK} onChange={setField('topK')} minValue={1}>
            <Label>Top K</Label>
            <Input placeholder="server default" />
          </NumberField>
        </div>
        <Button type="submit">
          <Search size={16} />
          Search
        </Button>
      </div>
    </Form>
  )
}
