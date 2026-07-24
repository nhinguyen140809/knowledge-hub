import { useQuery } from '@tanstack/react-query'
import { useActiveConnection } from '@/lib/store/connections.store'
import {
  fetchAccessGraph,
  fetchEffectivePermissions,
  fetchMembers,
  fetchPrincipal,
  fetchPrincipalGraph,
  fetchPrincipals,
} from '../api/principal.api'
import { accessKeys } from '../api/access.keys'

/** Principals plus their membership edges — one call, enough to draw the tree
 *  (including which principals are roots). */
export function usePrincipalGraph() {
  const active = useActiveConnection()
  return useQuery({
    queryKey: accessKeys.principalGraph(active?.id),
    queryFn: fetchPrincipalGraph,
    enabled: !!active,
  })
}

/** All principals (subjects and groups) on the active backend. */
export function usePrincipals() {
  const active = useActiveConnection()
  return useQuery({
    queryKey: accessKeys.principals(active?.id),
    queryFn: fetchPrincipals,
    enabled: !!active,
  })
}

/** A single principal by id. */
export function usePrincipal(id: string | undefined) {
  const active = useActiveConnection()
  return useQuery({
    queryKey: accessKeys.principal(active?.id, id ?? ''),
    queryFn: () => fetchPrincipal(id!),
    enabled: !!active && !!id,
  })
}

/** Direct member ids of a group. */
export function useMembers(groupId: string | undefined) {
  const active = useActiveConnection()
  return useQuery({
    queryKey: accessKeys.members(active?.id, groupId ?? ''),
    queryFn: () => fetchMembers(groupId!),
    enabled: !!active && !!groupId,
  })
}

/** The scoped subgraph explaining one principal's access — nodes, edges, done;
 *  the client only lays out and styles. */
export function useAccessGraph(principalId: string | undefined) {
  const active = useActiveConnection()
  return useQuery({
    queryKey: accessKeys.accessGraph(active?.id, principalId ?? ''),
    queryFn: () => fetchAccessGraph(principalId!),
    enabled: !!active && !!principalId,
  })
}

/** Read access resolved through groups and the default policy. */
export function useEffectivePermissions(principalId: string | undefined) {
  const active = useActiveConnection()
  return useQuery({
    queryKey: accessKeys.effectivePermissions(active?.id, principalId ?? ''),
    queryFn: () => fetchEffectivePermissions(principalId!),
    enabled: !!active && !!principalId,
  })
}
