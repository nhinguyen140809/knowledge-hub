import { Button } from '@heroui/react'
import { useQueryClient } from '@tanstack/react-query'
import { Database, KeyRound, RefreshCw } from 'lucide-react'
import { useAllCredentials } from '@/features/access'
import { useSources } from '@/features/sources'
import { StatCard } from '../components/StatCard'
import { SystemInfoPanel } from '../components/SystemInfoPanel'

/** Landing screen: runtime panel + at-a-glance counts, with a Sync that refreshes
 *  every backend query at once. Counts come from the owning features' public
 *  hooks, so those screens share one cache entry with this one. */
export function DashboardPage() {
  const queryClient = useQueryClient()
  const sources = useSources()
  const credentials = useAllCredentials()

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
        <StatCard
          label="Credentials"
          value={credentials.data?.length ?? 0}
          isLoading={credentials.isPending}
          icon={<KeyRound size={20} />}
        />
      </div>
    </div>
  )
}
