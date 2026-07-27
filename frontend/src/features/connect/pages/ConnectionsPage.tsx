import { Button } from '@heroui/react'
import { Cable, Plus } from 'lucide-react'
import { useConnectionStore } from '@/lib/store/connections.store'
import { EmptyState } from '@/shared/components/ui/EmptyState'
import { ROUTES } from '@/shared/constants'
import { renderLink } from '@/shared/lib/renderLink'
import { ConnectionRow } from '../components/ConnectionRow'

/** The backend connections registry: every connection this browser remembers,
 *  each renamable and removable. Adding one still goes through /connect,
 *  which validates it against the backend before it's saved — this page
 *  never duplicates that. */
export function ConnectionsPage() {
  const connections = useConnectionStore((s) => s.connections)
  const activeId = useConnectionStore((s) => s.activeId)

  return (
    <div className="flex flex-col">
      <div className="mx-auto flex w-full max-w-3xl flex-col gap-4">
        <div className="flex justify-end">
          <Button size="sm" variant="primary" render={renderLink(ROUTES.connect)}>
            <Plus size={16} />
            Connection
          </Button>
        </div>

        {connections.length === 0 ? (
          <EmptyState icon={<Cable size={28} />} description="No connections yet." />
        ) : (
          <div className="flex flex-col gap-2">
            {connections.map((c) => (
              <ConnectionRow key={c.id} connection={c} isActive={c.id === activeId} />
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
