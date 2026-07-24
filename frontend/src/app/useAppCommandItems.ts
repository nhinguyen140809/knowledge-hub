import { Database, FolderClosed, User, type LucideIcon } from 'lucide-react'
import { useMemo } from 'react'
import { usePrincipalGraph } from '@/features/access/hooks/usePrincipals'
import type { PrincipalType } from '@/features/access/types/access.type'
import { useSources } from '@/features/sources/hooks/useSources'
import type { CommandItem } from '@/shared/components/command-palette'

const PRINCIPAL_ICON: Record<PrincipalType, LucideIcon> = {
  GROUP: FolderClosed,
  SUBJECT: User,
}

/**
 * Feeds the command palette: flattens principals and sources into the neutral
 * {@link CommandItem} contract. This is the one place that knows both domains,
 * keeping the palette itself feature-agnostic. Reads the same cached queries the
 * Access and Sources pages use, so the list is warm the moment it opens.
 */
export function useAppCommandItems(): CommandItem[] {
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
        to: `/access?principal=${encodeURIComponent(p.principalId)}`,
        icon: PRINCIPAL_ICON[p.type],
      })
    }
    for (const s of sources.data ?? []) {
      items.push({
        key: `source:${s.id}`,
        label: s.name ?? s.id,
        hint: 'source',
        search: `${s.id} ${s.name ?? ''}`.toLowerCase(),
        to: `/sources/${encodeURIComponent(s.id)}`,
        icon: Database,
      })
    }
    return items
  }, [principals.data, sources.data])
}
