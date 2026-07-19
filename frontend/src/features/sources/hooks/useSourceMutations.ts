import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createSource, deleteSource, syncSource, updateSource } from '../api/sources.api'
import { sourceKeys } from '../api/sources.keys'
import type { CreateSourceInput, UpdateSourceInput } from '../types/source.type'

/** Every source mutation invalidates the whole source namespace: a create/delete
 *  changes the list, an update changes list + detail, and a sync changes status. */
function useInvalidateSources() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: sourceKeys.all })
}

export function useCreateSource() {
  const invalidate = useInvalidateSources()
  return useMutation({
    mutationFn: (input: CreateSourceInput) => createSource(input),
    onSuccess: invalidate,
  })
}

export function useUpdateSource() {
  const invalidate = useInvalidateSources()
  return useMutation({
    mutationFn: ({ id, input }: { id: string; input: UpdateSourceInput }) =>
      updateSource(id, input),
    onSuccess: invalidate,
  })
}

export function useDeleteSource() {
  const invalidate = useInvalidateSources()
  return useMutation({
    mutationFn: (id: string) => deleteSource(id),
    onSuccess: invalidate,
  })
}

export function useSyncSource() {
  const invalidate = useInvalidateSources()
  return useMutation({
    mutationFn: (id: string) => syncSource(id),
    onSuccess: invalidate,
  })
}
