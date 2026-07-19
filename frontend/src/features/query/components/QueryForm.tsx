import { Button, Input, Label, ListBox, NumberField, Select, TextField } from '@heroui/react'
import { Search } from 'lucide-react'
import { type FormEvent, useState } from 'react'
import type { HitType, QueryInput } from '../types/query.type'

const TYPES: HitType[] = ['code', 'doc', 'requirement', 'commit']

/** The search box plus the knobs the API accepts. All knobs *narrow* the search
 *  inside what the caller may already read — none of them can widen access. */
export function QueryForm({ onSubmit }: { onSubmit: (input: QueryInput) => void }) {
  const [text, setText] = useState('')
  const [sourceId, setSourceId] = useState('')
  const [ref, setRef] = useState('')
  const [type, setType] = useState<HitType | 'any'>('any')
  const [topK, setTopK] = useState<number | undefined>(undefined)

  function submit(e: FormEvent) {
    e.preventDefault()
    if (!text.trim()) return
    onSubmit({
      text: text.trim(),
      topK: topK ?? null,
      sourceId: sourceId.trim() || null,
      ref: ref.trim() || null,
      type: type === 'any' ? null : type,
    })
  }

  return (
    <form onSubmit={submit} className="flex flex-col gap-4">
      <div className="flex items-end gap-2">
        <TextField value={text} onChange={setText} className="flex-1" isRequired>
          <Label>Query</Label>
          <Input placeholder="How is the retrieval cache keyed?" />
        </TextField>
        <Button type="submit">
          <Search size={16} />
          Search
        </Button>
      </div>

      <div className="grid gap-3 sm:grid-cols-4">
        <TextField value={sourceId} onChange={setSourceId}>
          <Label>Source</Label>
          <Input placeholder="any" />
        </TextField>

        <TextField value={ref} onChange={setRef}>
          <Label>Ref</Label>
          <Input placeholder="canonical" />
        </TextField>

        <Select
          placeholder="Any type"
          selectedKey={type}
          onSelectionChange={(key) => setType(key as HitType | 'any')}
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

        <NumberField value={topK} onChange={setTopK} minValue={1}>
          <Label>Top K</Label>
          <Input placeholder="server default" />
        </NumberField>
      </div>
    </form>
  )
}
