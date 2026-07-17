import { useQuery } from '@tanstack/react-query'
import { useActiveConnection } from '@/lib/store/connections.store'
import { fetchSources } from '../api/sources.api'
import { sourceKeys } from '../api/sources.keys'

/** Sources for the active backend. Combines the api call with its query key and
 *  binds them to React Query; disabled until a backend is selected. */
export function useSources() {
  const active = useActiveConnection()
  return useQuery({
    queryKey: sourceKeys.list(active?.id),
    queryFn: fetchSources,
    enabled: !!active,
  })
}
