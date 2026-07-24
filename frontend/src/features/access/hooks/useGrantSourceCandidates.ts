import { useSources } from '@/features/sources'
import type { Source } from '@/features/sources'
import { useEffectivePermissions } from './usePrincipals'

/**
 * Sources eligible for a new direct grant to `principalId`: everything except
 * what is already directly granted. The direct set comes from the
 * origin-tagged effective permissions (already cached for the sources panel),
 * not a separate grants call. While either query is in flight the list is
 * provisional, so callers should disable selection until `isLoading` clears —
 * that also closes the window where an already-granted source could be
 * picked again.
 */
export function useGrantSourceCandidates(principalId: string | null): {
  candidates: Source[]
  isLoading: boolean
  isError: boolean
} {
  const sources = useSources()
  const permissions = useEffectivePermissions(principalId ?? undefined)

  const granted = new Set(
    (permissions.data?.sources ?? []).filter((s) => s.origin === 'DIRECT').map((s) => s.sourceId),
  )

  return {
    candidates: (sources.data ?? []).filter((s) => !granted.has(s.id)),
    isLoading: sources.isPending || permissions.isPending,
    isError: sources.isError,
  }
}
