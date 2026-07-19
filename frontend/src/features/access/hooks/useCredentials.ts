import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useActiveConnection } from '@/lib/store/connections.store'
import {
  fetchAllCredentials,
  fetchCredentials,
  issueCredential,
  revokeCredential,
} from '../api/access.api'
import { accessKeys } from '../api/access.keys'

/** Every credential across all principals — used for totals and global views. */
export function useAllCredentials() {
  const active = useActiveConnection()
  return useQuery({
    queryKey: accessKeys.allCredentials(active?.id),
    queryFn: fetchAllCredentials,
    enabled: !!active,
  })
}

/** A principal's credentials (metadata only — revoked ones are still listed). */
export function useCredentials(principalId: string | undefined) {
  const active = useActiveConnection()
  return useQuery({
    queryKey: accessKeys.credentials(active?.id, principalId ?? ''),
    queryFn: () => fetchCredentials(principalId!),
    enabled: !!active && !!principalId,
  })
}

/** Issues a credential. The returned `secret` is shown once and never again, so
 *  callers must surface it from the mutation result rather than refetching. */
export function useIssueCredential() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ principalId, name }: { principalId: string; name: string }) =>
      issueCredential(principalId, name),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: accessKeys.all }),
  })
}

/** Revokes a credential (soft-delete). */
export function useRevokeCredential() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (credentialId: string) => revokeCredential(credentialId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: accessKeys.all }),
  })
}
