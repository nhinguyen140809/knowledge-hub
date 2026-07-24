import { useQuery } from '@tanstack/react-query'
import { useActiveConnection } from '@/lib/store/connections.store'
import { runQuery } from '../api/query.api'
import { queryKeys } from '../api/query.keys'
import type { QueryInput } from '../types/query.type'

/**
 * Runs a retrieval query. Modelled as a query rather than a mutation because the
 * call is a read: pass `null` while nothing has been submitted, then pass the
 * submitted input — re-running the same search is served from cache.
 */
export function useSearch(input: QueryInput | null) {
  const active = useActiveConnection()
  return useQuery({
    queryKey: queryKeys.search(active?.id, input),
    queryFn: () => runQuery(input!),
    enabled: !!active && !!input?.text.trim(),
  })
}
