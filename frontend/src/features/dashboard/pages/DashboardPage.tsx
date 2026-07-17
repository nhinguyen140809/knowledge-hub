import { Button } from '@heroui/react'
import { useQueryClient } from '@tanstack/react-query'
import { Database, KeyRound, RefreshCw } from 'lucide-react'
import { useSources } from '@/features/sources'
import { StatCard } from '../components/StatCard'
import { SystemInfoPanel } from '../components/SystemInfoPanel'

// TODO: wire to a credentials API once the Access feature lands.
const MOCK_CREDENTIAL_COUNT = 3

/** Landing screen: runtime panel + at-a-glance counts, with a Sync that refreshes
 *  every backend query at once. Source data comes from the sources feature's
 *  public hook, so both screens share one cache entry. */
export function DashboardPage() {
  const queryClient = useQueryClient()
  const sources = useSources()

  return (
    <div className="flex flex-col gap-6">
      <div className="flex justify-end">
        <Button size="sm" variant="secondary" onPress={() => queryClient.invalidateQueries()}>
          <RefreshCw size={16} className={sources.isFetching ? 'animate-spin' : ''} />
          Sync
        </Button>
      </div>

      <SystemInfoPanel />

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard
          label="Sources"
          value={sources.data?.length ?? 0}
          isLoading={sources.isPending}
          icon={<Database size={20} />}
        />
        <StatCard label="Credentials" value={MOCK_CREDENTIAL_COUNT} icon={<KeyRound size={20} />} />
      </div>
    </div>
  )
}
