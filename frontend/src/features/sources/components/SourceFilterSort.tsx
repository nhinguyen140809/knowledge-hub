import { Button, Label, ListBox, Select } from '@heroui/react'
import { ArrowDownWideNarrow, ArrowUpNarrowWide, type LucideIcon } from 'lucide-react'
import { SOURCE_TYPE_LABEL } from '../constants/source.config'
import type {
  SourceFilterSortState,
  SourceSortDirection,
  SourceSortField,
  SourceTypeFilter,
} from '../lib/sourceFilterSort'

interface SourceFilterSortProps {
  value: SourceFilterSortState
  onChange: (value: SourceFilterSortState) => void
}

const TYPE_OPTIONS: { id: SourceTypeFilter; label: string }[] = [
  { id: 'ALL', label: 'All types' },
  { id: 'GIT', label: SOURCE_TYPE_LABEL.GIT },
  { id: 'FS', label: SOURCE_TYPE_LABEL.FS },
]

const SORT_OPTIONS: { id: SourceSortField; label: string }[] = [
  { id: 'lastUpdate', label: 'Last update' },
  { id: 'id', label: 'Id' },
]

const SORT_DIRECTION_CONFIG: Record<SourceSortDirection, { icon: LucideIcon; label: string }> = {
  asc: { icon: ArrowUpNarrowWide, label: 'Sort ascending' },
  desc: { icon: ArrowDownWideNarrow, label: 'Sort descending' },
}

/** Type filter + sort field/direction for the source list. Purely controlled
 *  — SourcesPage owns the state and applies it via applySourceFilterSort. */
export function SourceFilterSort({ value, onChange }: SourceFilterSortProps) {
  const direction = SORT_DIRECTION_CONFIG[value.sortDirection]
  const DirectionIcon = direction.icon

  function handleTypeChange(key: string | number | null) {
    if (key != null) onChange({ ...value, type: key as SourceTypeFilter })
  }

  function handleSortFieldChange(key: string | number | null) {
    if (key != null) onChange({ ...value, sortField: key as SourceSortField })
  }

  function toggleDirection() {
    onChange({ ...value, sortDirection: value.sortDirection === 'asc' ? 'desc' : 'asc' })
  }

  return (
    <div className="flex items-end gap-2">
      <Select
        selectedKey={value.type}
        onSelectionChange={handleTypeChange}
        variant="secondary"
        className="w-32"
      >
        <Label>Type</Label>
        <Select.Trigger>
          <Select.Value />
          <Select.Indicator />
        </Select.Trigger>
        <Select.Popover>
          <ListBox>
            {TYPE_OPTIONS.map((option) => (
              <ListBox.Item key={option.id} id={option.id} textValue={option.label}>
                {option.label}
                <ListBox.ItemIndicator />
              </ListBox.Item>
            ))}
          </ListBox>
        </Select.Popover>
      </Select>

      <Select
        selectedKey={value.sortField}
        onSelectionChange={handleSortFieldChange}
        variant="secondary"
        className="w-36"
      >
        <Label>Sort by</Label>
        <Select.Trigger>
          <Select.Value />
          <Select.Indicator />
        </Select.Trigger>
        <Select.Popover>
          <ListBox>
            {SORT_OPTIONS.map((option) => (
              <ListBox.Item key={option.id} id={option.id} textValue={option.label}>
                {option.label}
                <ListBox.ItemIndicator />
              </ListBox.Item>
            ))}
          </ListBox>
        </Select.Popover>
      </Select>

      <Button
        isIconOnly
        size="sm"
        variant="secondary"
        aria-label={direction.label}
        onPress={toggleDirection}
      >
        <DirectionIcon
          key={value.sortDirection}
          size={16}
          className="animate-in fade-in-0 duration-200"
        />
      </Button>
    </div>
  )
}
