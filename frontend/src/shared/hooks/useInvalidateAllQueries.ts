import { useQueryClient } from '@tanstack/react-query'

/** Invalidates every cached query across every feature — used by the
 *  dashboard's "Sync" action, which refreshes everything at once rather than
 *  one feature's namespace. */
export function useInvalidateAllQueries() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries()
}
