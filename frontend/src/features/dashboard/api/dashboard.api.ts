import { apiFetch } from '@/lib/api/axios'
import {
  mockDependencyHealth,
  mockKnowledgeStats,
  mockRetrievalStats,
} from '@/lib/api/mocks/dashboard.mock'
import { mockResolve } from '@/lib/api/mocks/mock.util'
import { mockSystemInfo } from '@/lib/api/mocks/system.mock'
import { isMock } from '@/lib/config'
import type {
  DependencyStatus,
  KnowledgeStats,
  RetrievalStats,
} from '@/features/dashboard/types/dashboard.type'
import type { SystemInfo } from '@/shared/types/system.type'

/** GET /system/info for the currently active connection. */
export function fetchSystemInfo(): Promise<SystemInfo> {
  if (isMock) return mockResolve(mockSystemInfo)
  return apiFetch<SystemInfo>('/system/info')
}

/** GET /system/knowledge-stats — how much is indexed (graph + vectors). */
export function fetchKnowledgeStats(): Promise<KnowledgeStats> {
  if (isMock) return mockResolve(mockKnowledgeStats)
  return apiFetch<KnowledgeStats>('/system/knowledge-stats')
}

/** GET /system/dependencies — reachability of Neo4j, Qdrant, the embedding
 *  provider. A thin wrapper the backend fills from actuator health components,
 *  kept under the API prefix so it shares the same auth as everything else. */
export function fetchDependencyHealth(): Promise<DependencyStatus[]> {
  if (isMock) return mockResolve(mockDependencyHealth)
  return apiFetch<DependencyStatus[]>('/system/dependencies')
}

/** GET /system/retrieval-stats — retrieval activity since process start. */
export function fetchRetrievalStats(): Promise<RetrievalStats> {
  if (isMock) return mockResolve(mockRetrievalStats)
  return apiFetch<RetrievalStats>('/system/retrieval-stats')
}
