import { Button } from '@heroui/react'
import { Files, Waypoints, RefreshCw, ScatterChart, Spline } from 'lucide-react'
import { useInvalidateAllQueries } from '@/shared/hooks/useInvalidateAllQueries'
import { NO_VALUE } from '@/shared/constants'
import { AttentionPanel } from '../components/AttentionPanel'
import { RetrievalPanel } from '../components/RetrievalPanel'
import { ServicesPanel } from '../components/ServicesPanel'
import { StatCard } from '../components/StatCard'
import { SystemInfoPanel } from '../components/SystemInfoPanel'
import { useKnowledgeStats } from '../hooks/useDashboardStats'

/** Formats a count with thousands separators */
const fmt = (n: number | undefined) => (n == null ? NO_VALUE : n.toLocaleString())

/**
 * Landing screen for the product: the scale of the knowledge base (documents,
 * graph, vectors), how retrieval is performing, the runtime and its
 * dependencies, and anything that needs a look.
 */
export function DashboardPage() {
  const refreshAll = useInvalidateAllQueries()
  const knowledge = useKnowledgeStats()

  return (
    <div className="flex flex-col gap-6">
      <div className="flex justify-end">
        <Button size="sm" variant="secondary" onPress={refreshAll}>
          <RefreshCw size={16} className={knowledge.isFetching ? 'animate-spin' : ''} />
          Refresh
        </Button>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard
          label="Documents"
          value={fmt(knowledge.data?.documents)}
          isLoading={knowledge.isPending}
          icon={<Files size={20} />}
        />
        <StatCard
          label="Graph nodes"
          value={fmt(knowledge.data?.graphNodes)}
          isLoading={knowledge.isPending}
          icon={<Waypoints size={20} />}
        />
        <StatCard
          label="Relationships"
          value={fmt(knowledge.data?.graphEdges)}
          isLoading={knowledge.isPending}
          icon={<Spline size={20} />}
        />
        <StatCard
          label="Vectors"
          value={fmt(knowledge.data?.vectors)}
          isLoading={knowledge.isPending}
          icon={<ScatterChart size={20} />}
        />
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <div className="flex flex-col gap-4">
          <SystemInfoPanel />
          <ServicesPanel />
        </div>
        <div className="flex flex-col gap-4">
          <RetrievalPanel />
          <AttentionPanel />
        </div>
      </div>
    </div>
  )
}
