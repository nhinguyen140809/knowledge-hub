import { useQuery } from '@tanstack/react-query'
import { useActiveConnection } from '@/lib/store/connections.store'
import { accessKeys } from '../../shared/api/access.keys'
import { fetchSourceAccessGraph, fetchSourcePrincipals } from '../api/source.api'

/** Every principal that can read a source, with its access origin — the
 *  inverse of a principal's effective permissions. */
export function useSourcePrincipals(sourceId: string | undefined) {
  const active = useActiveConnection()
  return useQuery({
    queryKey: accessKeys.sourcePrincipals(active?.id, sourceId ?? ''),
    queryFn: () => fetchSourcePrincipals(sourceId!),
    enabled: !!active && !!sourceId,
  })
}

/** The scoped subgraph explaining who can read a source — nodes, edges, done;
 *  the client only lays out and styles. */
export function useSourceAccessGraph(sourceId: string | undefined) {
  const active = useActiveConnection()
  return useQuery({
    queryKey: accessKeys.sourceAccessGraph(active?.id, sourceId ?? ''),
    queryFn: () => fetchSourceAccessGraph(sourceId!),
    enabled: !!active && !!sourceId,
  })
}
