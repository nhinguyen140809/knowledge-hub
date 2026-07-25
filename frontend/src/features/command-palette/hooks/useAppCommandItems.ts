import { Database, FolderClosed, User, type LucideIcon } from 'lucide-react'
import { useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { usePrincipalGraph } from '@/features/access/hooks/usePrincipals'
import type { PrincipalType } from '@/features/access/types/access.type'
import { useSources } from '@/features/sources/hooks/useSources'
import type { CommandItem } from '@/shared/components/ui/command-palette'

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
 * same one twice still re-selects it; a source opens its detail route.
 */
export function useAppCommandItems(): CommandItem[] {
  const navigate = useNavigate()
  const principals = usePrincipalGraph()
  const sources = useSources()

  return useMemo(() => {
    const items: CommandItem[] = []
    for (const p of principals.data?.principals ?? []) {
      items.push({
        key: `principal:${p.principalId}`,
        label: p.principalId,
        hint: p.type === 'GROUP' ? 'group' : 'subject',
        search: p.principalId.toLowerCase(),
        icon: PRINCIPAL_ICON[p.type],
        action: () => navigate('/access', { state: { selectPrincipal: p.principalId } }),
      })
    }
    for (const s of sources.data ?? []) {
      items.push({
        key: `source:${s.id}`,
        label: s.name ?? s.id,
        hint: 'source',
        search: `${s.id} ${s.name ?? ''}`.toLowerCase(),
        icon: Database,
        action: () => navigate(`/sources/${encodeURIComponent(s.id)}`),
      })
    }
    return items
  }, [principals.data, sources.data, navigate])
}
