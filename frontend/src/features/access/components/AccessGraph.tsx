import { Skeleton } from '@heroui/react'
import { type Edge, type Node } from '@xyflow/react'
import { useMemo } from 'react'
import { GraphView } from '@/shared/components/ui/GraphView'
import { useEffectivePermissions, usePrincipalGraph } from '../hooks/usePrincipals'

interface AccessGraphProps {
  selectedId?: string | null
  onSelect?: (principalId: string) => void
}

/**
 * Membership and access as the graph it actually is: one node per principal, no
 * duplication, cycles drawn as loops. When a principal is selected, the sources
 * it can read are added along with the edge that grants each one — which is the
 * question a tree cannot answer, "*why* does this principal reach that source".
 */
export function AccessGraph({ selectedId, onSelect }: AccessGraphProps) {
  const graph = usePrincipalGraph()
  const permissions = useEffectivePermissions(selectedId ?? undefined)

  const { nodes, edges } = useMemo(() => {
    const nodes: Node[] = []
    const edges: Edge[] = []
    if (!graph.data) return { nodes, edges }

    for (const principal of graph.data.principals) {
      const isSelected = principal.principalId === selectedId
      const isGroup = principal.type === 'GROUP'
      nodes.push({
        id: principal.principalId,
        position: { x: 0, y: 0 },
        data: { label: `${principal.principalId}${isGroup ? ' (group)' : ''}` },
        style: {
          borderRadius: 10,
          borderWidth: isSelected ? 2 : 1,
          borderStyle: isGroup ? 'solid' : 'dashed',
          fontSize: 12,
          padding: 6,
        },
        className: isSelected ? 'text-accent' : undefined,
      })
    }

    for (const [groupId, memberIds] of Object.entries(graph.data.membership)) {
      for (const memberId of memberIds) {
        edges.push({
          id: `member:${groupId}->${memberId}`,
          source: groupId,
          target: memberId,
          label: 'member',
          style: { strokeDasharray: '4 2' },
        })
      }
    }

    // grantedVia maps a source to the principals that grant it, so each entry is
    // exactly one "this is where the access comes from" edge.
    if (permissions.data) {
      for (const [sourceId, viaPrincipals] of Object.entries(permissions.data.grantedVia)) {
        nodes.push({
          id: `source:${sourceId}`,
          position: { x: 0, y: 0 },
          data: { label: sourceId },
          style: { borderRadius: 10, fontSize: 12, padding: 6 },
        })
        for (const via of viaPrincipals) {
          edges.push({
            id: `grant:${via}->${sourceId}`,
            source: via,
            target: `source:${sourceId}`,
            label: 'grants',
            animated: true,
          })
        }
      }
    }

    return { nodes, edges }
  }, [graph.data, permissions.data, selectedId])

  if (graph.isPending) return <Skeleton className="h-[420px] w-full rounded-xl" />
  if (graph.isError) return <p className="text-danger text-sm">{(graph.error as Error).message}</p>

  return (
    <GraphView
      nodes={nodes}
      edges={edges}
      onNodeClick={(id) => !id.startsWith('source:') && onSelect?.(id)}
    />
  )
}
