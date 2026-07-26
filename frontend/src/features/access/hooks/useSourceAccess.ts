import { useQuery } from '@tanstack/react-query'
import { useActiveConnection } from '@/lib/store/connections.store'
import { accessKeys } from '../api/access.keys'
import { fetchSourcePrincipals } from '../api/source.api'

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
