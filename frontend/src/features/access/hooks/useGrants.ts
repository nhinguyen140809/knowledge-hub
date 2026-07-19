import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useActiveConnection } from '@/lib/store/connections.store'
import { fetchGrants, grantSources, revokeSources } from '../api/access.api'
import { accessKeys } from '../api/access.keys'
import type { GrantInput } from '../types/access.type'

/** Source ids granted directly to a principal (inherited ones are not included —
 *  use useEffectivePermissions for the resolved set). */
export function useGrants(principalId: string | undefined) {
  const active = useActiveConnection()
  return useQuery({
    queryKey: accessKeys.grants(active?.id, principalId ?? ''),
    queryFn: () => fetchGrants(principalId!),
    enabled: !!active && !!principalId,
  })
}

export function useGrantSources() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: GrantInput) => grantSources(input),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: accessKeys.all }),
  })
}

export function useRevokeSources() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: GrantInput) => revokeSources(input),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: accessKeys.all }),
  })
}
