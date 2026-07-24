import { apiFetch } from '@/lib/api/axios'
import { mockResolve } from '@/lib/api/mocks/mock.util'
import { isMock } from '@/lib/config'
import type { QueryInput, RankedResult } from '../types/query.type'
import { mockRankedResult } from './query.mock'

/** POST /query — hybrid (semantic + keyword + graph) search. It is a POST
 *  because the query is a body, but semantically a read: same input, same
 *  result, so it is safe to cache under a query key. */
export function runQuery(input: QueryInput): Promise<RankedResult> {
  if (isMock) return mockResolve(mockRankedResult)
  return apiFetch<RankedResult>('/query', { method: 'POST', data: input })
}
