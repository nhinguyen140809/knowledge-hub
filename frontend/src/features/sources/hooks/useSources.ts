import { useQuery } from '@tanstack/react-query'
import { useActiveConnection } from '@/lib/store/connections.store'
import { fetchSource, fetchSources, fetchSourceStatus } from '../api/sources.api'
import { sourceKeys } from '../api/sources.keys'

/** All sources on the active backend. Disabled until a backend is selected. */
export function useSources() {
  const active = useActiveConnection()
  return useQuery({
    queryKey: sourceKeys.list(active?.id),
    queryFn: fetchSources,
    enabled: !!active,
  })
}

/** A single source by id. */
export function useSource(id: string | undefined) {
  const active = useActiveConnection()
  return useQuery({
    queryKey: sourceKeys.detail(active?.id, id ?? ''),
    queryFn: () => fetchSource(id!),
    enabled: !!active && !!id,
  })
}

/** Index freshness for a source — the "last update" shown next to it. */
export function useSourceStatus(id: string | undefined) {
  const active = useActiveConnection()
  return useQuery({
    queryKey: sourceKeys.status(active?.id, id ?? ''),
    queryFn: () => fetchSourceStatus(id!),
    enabled: !!active && !!id,
  })
}
