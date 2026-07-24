import type {
  DependencyStatus,
  KnowledgeStats,
  RetrievalStats,
} from '@/features/dashboard/types/dashboard.type'

/** Stand-ins for the dashboard's aggregate endpoints when VITE_API_MODE=mock,
 *  sized so the numbers look plausible together (vectors ≈ documents, a graph
 *  several times larger in edges than nodes). */
export const mockKnowledgeStats: KnowledgeStats = {
  documents: 1240,
  graphNodes: 8300,
  graphEdges: 19400,
  vectors: 1240,
}

/** One service DOWN so the "Needs attention" panel has something to show in
 *  mock mode. */
export const mockDependencyHealth: DependencyStatus[] = [
  { name: 'neo4j', status: 'UP' },
  { name: 'qdrant', status: 'UP' },
  { name: 'embeddings', status: 'DOWN' },
]

export const mockRetrievalStats: RetrievalStats = {
  queriesServed: 4210,
  p50LatencyMs: 42,
  p95LatencyMs: 180,
  cacheHitRate: 0.73,
}
