import { useMutation, useQueryClient } from '@tanstack/react-query'
import { grantSources, revokeSources } from '../api/grant.api'
import { accessKeys } from '../api/access.keys'
import type { GrantInput } from '../types/access.type'

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
