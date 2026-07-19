import { Chip, Skeleton } from '@heroui/react'
import { RotateCcw, User, Users } from 'lucide-react'
import { Tree } from '@/shared/components/ui/Tree'
import { usePrincipalGraph } from '../hooks/usePrincipals'
import type { Principal } from '../types/access.type'

interface PrincipalTreeProps {
  selectedId?: string | null
  onSelect?: (principalId: string) => void
}

const icon = (principal: Principal) =>
  principal.type === 'GROUP' ? <Users size={15} /> : <User size={15} />

const roleChip = (principal: Principal) =>
  principal.role === 'ADMIN' ? (
    <Chip size="sm" variant="soft" color="accent">
      admin
    </Chip>
  ) : null

/**
 * The principal hierarchy. Membership is a directed graph, not a tree: groups
 * nest, a principal can belong to several groups, and the backend does not
 * reject cycles. So nodes are keyed by their path (the same principal may render
 * under several parents) and recursion stops when a principal repeats on the
 * current path — otherwise a cycle would render forever.
 */
export function PrincipalTree({ selectedId, onSelect }: PrincipalTreeProps) {
  const { data, isPending, isError, error } = usePrincipalGraph()

  if (isPending) {
    return (
      <div className="flex flex-col gap-2">
        {Array.from({ length: 5 }).map((_, i) => (
          <Skeleton key={i} className="h-8 w-full rounded-lg" />
        ))}
      </div>
    )
  }

  if (isError) return <p className="text-danger text-sm">{(error as Error).message}</p>

  const byId = new Map(data.principals.map((p) => [p.principalId, p]))
  const membership = data.membership
  const hasParent = new Set(Object.values(membership).flat())
  const roots = data.principals.filter((p) => !hasParent.has(p.principalId))

  // A graph where every principal has a parent (a cycle with no entry point)
  // would leave no roots; show everything rather than an empty panel.
  const topLevel = roots.length > 0 ? roots : data.principals

  function renderNode(principal: Principal, path: string[]): ReturnType<typeof Tree.Item> {
    const key = path.join('/')
    const childIds = membership[principal.principalId] ?? []
    const row = {
      label: principal.principalId,
      icon: icon(principal),
      trailing: roleChip(principal),
      isSelected: selectedId === principal.principalId,
      onSelect: () => onSelect?.(principal.principalId),
    }

    if (childIds.length === 0) return <Tree.Item key={key} {...row} />

    return (
      <Tree.Group key={key} defaultExpanded {...row}>
        {childIds.map((childId) => {
          const child = byId.get(childId)
          if (!child) return null
          if (path.includes(childId)) {
            return (
              <Tree.Item
                key={`${key}/${childId}`}
                label={childId}
                icon={icon(child)}
                trailing={
                  <Chip size="sm" variant="soft" color="warning">
                    <RotateCcw size={12} />
                    cycle
                  </Chip>
                }
              />
            )
          }
          return renderNode(child, [...path, childId])
        })}
      </Tree.Group>
    )
  }

  return <Tree>{topLevel.map((p) => renderNode(p, [p.principalId]))}</Tree>
}
