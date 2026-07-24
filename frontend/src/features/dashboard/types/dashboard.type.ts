/** Scale of the knowledge base — GET /api/v1/system/knowledge-stats. The hero
 *  numbers of the dashboard: how much has been indexed. `documents` and
 *  `vectors` should track each other; a gap means embedding lags the graph. */
export interface KnowledgeStats {
  documents: number
  graphNodes: number
  graphEdges: number
  vectors: number
}

/** One dependency's reachability. Mirrors an actuator health component,
 *  normalized to the parts the UI shows. */
export type DependencyState = 'UP' | 'DOWN' | 'UNKNOWN'

export interface DependencyStatus {
  name: string
  status: DependencyState
}

/** Retrieval activity — GET /api/v1/system/retrieval-stats. Counts are since
 *  process start, not all-time; the UI labels them so. */
export interface RetrievalStats {
  queriesServed: number
  p50LatencyMs: number
  p95LatencyMs: number
  /** 0..1; share of queries answered from the retrieval cache. */
  cacheHitRate: number
}
