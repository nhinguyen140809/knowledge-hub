import { Button } from '@heroui/react'
import { Boxes, Network, RefreshCw, Search, Sparkles } from 'lucide-react'
import { useInvalidateAllQueries } from '@/shared/hooks/useInvalidateAllQueries'
import { NO_VALUE } from '@/shared/constants'
import { AttentionPanel } from '../components/AttentionPanel'
import { ServicesPanel } from '../components/ServicesPanel'
import { StatCard } from '../components/StatCard'
import { SystemInfoPanel } from '../components/SystemInfoPanel'
import { useKnowledgeStats, useRetrievalStats } from '../hooks/useDashboardStats'

/** Formats a count with thousands separators */
const fmt = (n: number | undefined) => (n == null ? NO_VALUE : n.toLocaleString())

/**
 * Landing screen for the product: the size and activity of the knowledge base
 * (indexed documents, graph, vectors, retrieval), the runtime and its
 * dependencies, and anything that needs a look. 
 */
export function DashboardPage() {
  const refreshAll = useInvalidateAllQueries()
  const knowledge = useKnowledgeStats()
  const retrieval = useRetrievalStats()

  return (
    <div className="flex flex-col gap-6">
      <div className="flex justify-end">
        <Button size="sm" variant="secondary" onPress={refreshAll}>
          <RefreshCw size={16} className={knowledge.isFetching ? 'animate-spin' : ''} />
          Sync
        </Button>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard
          label="Documents"
          value={fmt(knowledge.data?.documents)}
          isLoading={knowledge.isPending}
          icon={<Boxes size={20} />}
        />
        <StatCard
          label="Graph nodes"
          value={fmt(knowledge.data?.graphNodes)}
          isLoading={knowledge.isPending}
          icon={<Network size={20} />}
        />
        <StatCard
          label="Vectors"
          value={fmt(knowledge.data?.vectors)}
          isLoading={knowledge.isPending}
          icon={<Sparkles size={20} />}
        />
        <StatCard
          label="Queries served"
          value={fmt(retrieval.data?.queriesServed)}
          isLoading={retrieval.isPending}
          icon={<Search size={20} />}
        />
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <div className="flex flex-col gap-4">
          <SystemInfoPanel />
          <ServicesPanel />
        </div>
        <AttentionPanel />
      </div>
    </div>
  )
}
