import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useActiveConnection } from '@/lib/store/connections.store'
import { fetchDefaultPolicy, setDefaultPolicy } from '../api/policy.api'
import { accessKeys } from '../api/access.keys'
import type { DefaultPolicy } from '../types/access.type'

/** The system-wide fallback policy applied when no grant matches. */
export function useDefaultPolicy() {
  const active = useActiveConnection()
  return useQuery({
    queryKey: accessKeys.defaultPolicy(active?.id),
    queryFn: fetchDefaultPolicy,
    enabled: !!active,
  })
}

/** Flipping the default policy changes everyone's effective permissions, hence
 *  the namespace-wide invalidation. */
export function useSetDefaultPolicy() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (policy: DefaultPolicy) => setDefaultPolicy(policy),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: accessKeys.all }),
  })
}
