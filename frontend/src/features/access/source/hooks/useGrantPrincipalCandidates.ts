import { usePrincipals } from '../../principal/hooks/usePrincipals'
import type { Principal } from '../../shared/types/access.type'
import { canReceiveGrants } from '../../shared/lib/grant.rules'
import { useSourcePrincipals } from './useSourceAccess'

/**
 * Principals eligible for a new direct grant to `sourceId`: everyone except
 * whoever already has a direct grant, and admins (a grant to one is dead
 * config, their role already reads everything regardless). The direct set
 * comes from the origin-tagged source principals (already cached for the
 * panel), not a separate grants call. Mirrors the principal side's
 * `useGrantSourceCandidates`, principal and source swapped — admin exclusion
 * happens here rather than by disabling the whole action, since a source has
 * no single "the principal" to check ahead of the picker being opened.
 */
export function useGrantPrincipalCandidates(sourceId: string | null): {
  candidates: Principal[]
  isLoading: boolean
  isError: boolean
} {
  const principals = usePrincipals()
  const sourcePrincipals = useSourcePrincipals(sourceId ?? undefined)

  const granted = new Set(
    (sourcePrincipals.data?.principals ?? [])
      .filter((p) => p.origin === 'DIRECT')
      .map((p) => p.principalId),
  )

  return {
    candidates: (principals.data ?? []).filter(
      (p) => !granted.has(p.principalId) && canReceiveGrants(p),
    ),
    isLoading: principals.isPending || sourcePrincipals.isPending,
    isError: principals.isError,
  }
}
