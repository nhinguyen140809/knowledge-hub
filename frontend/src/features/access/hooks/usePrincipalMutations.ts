import { useMutation, useQueryClient } from '@tanstack/react-query'
import { addMember, createPrincipal, deletePrincipal, removeMember } from '../api/principal.api'
import { accessKeys } from '../api/access.keys'
import type { CreatePrincipalInput } from '../types/access.type'

/** Access data is interdependent (membership feeds effective permissions), so
 *  every mutation invalidates the whole access namespace rather than guessing. */
function useInvalidateAccess() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: accessKeys.all })
}

export function useCreatePrincipal() {
  const invalidate = useInvalidateAccess()
  return useMutation({
    mutationFn: (input: CreatePrincipalInput) => createPrincipal(input),
    onSuccess: invalidate,
  })
}

export function useDeletePrincipal() {
  const invalidate = useInvalidateAccess()
  return useMutation({
    mutationFn: (id: string) => deletePrincipal(id),
    onSuccess: invalidate,
  })
}

export function useAddMember() {
  const invalidate = useInvalidateAccess()
  return useMutation({
    mutationFn: ({ groupId, memberId }: { groupId: string; memberId: string }) =>
      addMember(groupId, memberId),
    onSuccess: invalidate,
  })
}

export function useRemoveMember() {
  const invalidate = useInvalidateAccess()
  return useMutation({
    mutationFn: ({ groupId, memberId }: { groupId: string; memberId: string }) =>
      removeMember(groupId, memberId),
    onSuccess: invalidate,
  })
}
