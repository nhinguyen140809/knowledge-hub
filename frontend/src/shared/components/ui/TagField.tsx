import { Button, Input, Label, Tag, TagGroup } from '@heroui/react'
import { Plus } from 'lucide-react'
import { type KeyboardEvent, useState } from 'react'

interface TagFieldProps {
  label: string
  value: string[]
  onChange: (value: string[]) => void
  placeholder?: string
}

/** A field for a short list of freeform string values (globs, tags, ...):
 *  existing entries render as removable chips, and a draft input at the
 *  bottom of the same box adds one on Enter or via the + button. Duplicate
 *  entries are ignored rather than added twice. */
export function TagField({ label, value, onChange, placeholder }: TagFieldProps) {
  const [draft, setDraft] = useState<string>('')

  function commitDraft() {
    const next = draft.trim()
    setDraft('')
    if (!next || value.includes(next)) return
    onChange([...value, next])
  }

  function onKeyDown(e: KeyboardEvent<HTMLInputElement>) {
    if (e.key !== 'Enter') return
    e.preventDefault()
    commitDraft()
  }

  return (
    <div className="flex flex-col gap-1.5">
      <Label>{label}</Label>
      <div className="bg-field rounded-field focus-within:border-field-border-focus focus-within:ring-focus transition:duration-200 flex flex-col gap-2 border p-2 transition-all outline-none focus-within:ring-2 focus-within:ring-offset-0">
        {value.length > 0 && (
          <TagGroup
            aria-label={label}
            onRemove={(keys) => onChange(value.filter((tag) => !keys.has(tag)))}
          >
            <TagGroup.List className="gap-1.5 px-1">
              {value.map((tag) => (
                <Tag key={tag} id={tag} textValue={tag}>
                  {tag}
                </Tag>
              ))}
            </TagGroup.List>
          </TagGroup>
        )}
        <div className="flex items-center gap-1">
          <Input
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            onKeyDown={onKeyDown}
            placeholder={placeholder}
            aria-label={`Add to ${label}`}
            className="flex-1 border-none bg-transparent px-1 shadow-none focus-visible:ring-0"
          />
          <Button
            isIconOnly
            size="sm"
            variant="ghost"
            isDisabled={!draft.trim()}
            onPress={commitDraft}
            aria-label={`Add ${label} entry`}
          >
            <Plus size={14} />
          </Button>
        </div>
      </div>
    </div>
  )
}
