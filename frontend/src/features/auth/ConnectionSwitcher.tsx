import { Button } from '@heroui/react'
import { useNavigate } from 'react-router-dom'
import { useConnectionStore } from '../../lib/store/connections.store'

/** Header control to switch the active backend or add a new one. A native
 *  <select> keeps this dependency-light; richer HeroUI inputs come later. */
export function ConnectionSwitcher() {
  const navigate = useNavigate()
  const connections = useConnectionStore((s) => s.connections)
  const activeId = useConnectionStore((s) => s.activeId)
  const setActive = useConnectionStore((s) => s.setActive)

  return (
    <div className="flex items-center gap-2">
      <select
        className="rounded-md border border-neutral-300 bg-white px-2 py-1 text-sm dark:border-neutral-700 dark:bg-neutral-900"
        value={activeId ?? ''}
        onChange={(e) => setActive(e.target.value)}
      >
        {connections.map((c) => (
          <option key={c.id} value={c.id}>
            {c.label} — {c.baseUrl}
          </option>
        ))}
      </select>
      <Button type="button" onPress={() => navigate('/connect')}>
        + Add
      </Button>
    </div>
  )
}
