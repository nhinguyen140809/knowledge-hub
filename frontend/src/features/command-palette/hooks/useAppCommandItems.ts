import { Database, FolderClosed, KeyRound, User, type LucideIcon } from 'lucide-react'
import { useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  navigateToPrincipal,
  navigateToSource,
  useAllCredentials,
  usePrincipalGraph,
  type PrincipalType,
} from '@/features/access'
import { useSources } from '@/features/sources/hooks/useSources'
import type { CommandItem } from '@/shared/components/ui/command-palette'
import { ROUTES } from '@/shared/constants'

const PRINCIPAL_ICON: Record<PrincipalType, LucideIcon> = {
  GROUP: FolderClosed,
  SUBJECT: User,
}

/**
 * Turns this product's principals and sources into command-palette items. The
 * only place that knows both domains *and* how selecting one navigates — the
 * generic palette knows neither. Reads the same cached queries the Access and
 * Sources pages use, so the list is warm the moment the palette opens.
 *
 * A principal is opened via navigation state (not a URL param) so choosing the
 * same one twice still re-selects it; a source gets two entries the same way —
 * one to its detail route, one to its access page — since both are equally
 * likely destinations and neither implies the other. A credential has no page
 * of its own, so selecting one opens its *owning* principal the same way — the
 * credential lives in that principal's panel.
 */
export function useAppCommandItems(): CommandItem[] {
  const navigate = useNavigate()
  const principals = usePrincipalGraph()
  const sources = useSources()
  const credentials = useAllCredentials()

  return useMemo(() => {
    const items: CommandItem[] = []
    for (const p of principals.data?.principals ?? []) {
      items.push({
        key: `principal:${p.principalId}`,
        label: p.principalId,
        hint: p.type === 'GROUP' ? 'group' : 'subject',
        search: p.principalId.toLowerCase(),
        icon: PRINCIPAL_ICON[p.type],
        action: () => navigateToPrincipal(navigate, p.principalId),
      })
    }
    for (const s of sources.data ?? []) {
      items.push({
        key: `source:${s.id}`,
        label: s.name ?? s.id,
        hint: 'source',
        search: `${s.id} ${s.name ?? ''}`.toLowerCase(),
        icon: Database,
        action: () => navigate(ROUTES.sourceDetail(s.id)),
      })
      items.push({
        key: `source-access:${s.id}`,
        label: s.name ?? s.id,
        hint: 'access',
        search: `${s.id} ${s.name ?? ''} access`.toLowerCase(),
        icon: KeyRound,
        action: () => navigateToSource(navigate, s.id),
      })
    }
    for (const c of credentials.data ?? []) {
      items.push({
        key: `credential:${c.credentialId}`,
        label: c.name,
        hint: c.principalId,
        // Not the credentialId: it's an opaque id, not something anyone types.
        search: c.name.toLowerCase(),
        icon: KeyRound,
        action: () => navigateToPrincipal(navigate, c.principalId),
      })
    }
    return items
  }, [principals.data, sources.data, credentials.data, navigate])
}
