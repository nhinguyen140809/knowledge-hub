import { Description, Label, ListBox, Select, Separator } from '@heroui/react'
import { Plus } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { useConnectionStore } from '@/lib/store/connections.store'

const ADD_CONNECTION = '__add-connection__'

/** Header control to switch the active backend. Adding a new one is just
 *  another option in the same select, rather than a separate button next
 *  to it — picking it navigates to /connect instead of setting the active id. */
export function ConnectionSwitcher() {
  const navigate = useNavigate()
  const connections = useConnectionStore((s) => s.connections)
  const activeId = useConnectionStore((s) => s.activeId)
  const setActive = useConnectionStore((s) => s.setActive)

  return (
    <Select
      fullWidth
      placeholder="Select a backend"
      selectedKey={activeId}
      onSelectionChange={(key) => {
        if (key === ADD_CONNECTION) navigate('/connect')
        else setActive(String(key))
      }}
    >
      <Label>Current connection</Label>
      <Select.Trigger>
        <Select.Value />
        <Select.Indicator />
      </Select.Trigger>
      <Select.Popover className="w-[var(--trigger-width)]">
        <ListBox>
          {connections.map((c) => (
            <ListBox.Item key={c.id} id={c.id} textValue={c.label}>
              <div className="flex min-w-0 flex-1 flex-col">
                <Label className="w-full truncate">{c.label}</Label>
                <Description className="font-mono">{c.baseUrl}</Description>
              </div>
              <ListBox.ItemIndicator />
            </ListBox.Item>
          ))}
          {connections.length > 0 && <Separator />}
          <ListBox.Item id={ADD_CONNECTION} textValue="Add connection">
            <Plus size={14} aria-hidden />
            <Label>Add connection</Label>
          </ListBox.Item>
        </ListBox>
      </Select.Popover>
    </Select>
  )
}
