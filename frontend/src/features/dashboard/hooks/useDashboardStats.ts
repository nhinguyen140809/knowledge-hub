import { useQuery } from '@tanstack/react-query'
import { useActiveConnection } from '@/lib/store/connections.store'
import {
  fetchDependencyHealth,
  fetchKnowledgeStats,
  fetchRetrievalStats,
} from '../api/dashboard.api'
import { dashboardKeys } from '../api/dashboard.keys'

/** Knowledge-base scale for the top stat tiles. Disabled until a backend is
 *  chosen, like every dashboard read. */
export function useKnowledgeStats() {
  const active = useActiveConnection()
  return useQuery({
    queryKey: dashboardKeys.knowledgeStats(active?.id),
    queryFn: fetchKnowledgeStats,
    enabled: !!active,
  })
}

/** Reachability of the backend's dependencies (Neo4j, Qdrant, embeddings). */
export function useDependencyHealth() {
  const active = useActiveConnection()
  return useQuery({
    queryKey: dashboardKeys.dependencyHealth(active?.id),
    queryFn: fetchDependencyHealth,
    enabled: !!active,
  })
}

/** Retrieval activity since process start. */
export function useRetrievalStats() {
  const active = useActiveConnection()
  return useQuery({
    queryKey: dashboardKeys.retrievalStats(active?.id),
    queryFn: fetchRetrievalStats,
    enabled: !!active,
  })
}
