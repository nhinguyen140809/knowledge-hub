import { Skeleton } from '@heroui/react'
import { MarkerType, type Edge, type Node } from '@xyflow/react'
import { useMemo } from 'react'
import {
  GraphView,
  type GraphNodeData,
  type GraphNodeVariant,
} from '@/shared/components/ui/GraphView'
import { ErrorState } from '@/shared/components/ui/ErrorState'
import { useEffectivePermissions, usePrincipalGraph } from '../hooks/usePrincipals'
import type { Principal } from '../types/access.type'

interface AccessGraphProps {
  selectedId?: string | null
  onSelect?: (principalId: string) => void
}

// Which variant a node *kind* gets — GraphView owns what each variant looks
// like, this only picks one per kind.
const PRINCIPAL_NODE_VARIANT: Record<Principal['type'], GraphNodeVariant> = {
  GROUP: 'accent',
  SUBJECT: 'neutral',
}
const SOURCE_NODE_VARIANT: GraphNodeVariant = 'success'

// Membership (structural, "belongs to") and grants (access, "can read") are
// the two kinds of edge in this graph — distinct color and marker size make
// that difference legible at a glance instead of only in the hover label.
const MEMBER_EDGE_COLOR = 'var(--muted)'
const GRANT_EDGE_COLOR = 'var(--success)'
const EDGE_MARKER_SIZE = 14

function edgeMarker(color: string) {
  return { type: MarkerType.Arrow, color, width: EDGE_MARKER_SIZE, height: EDGE_MARKER_SIZE }
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
    const nodes: Node<GraphNodeData>[] = []
    const edges: Edge[] = []
    if (!graph.data) return { nodes, edges }

    for (const principal of graph.data.principals) {
      const isSelected = principal.principalId === selectedId
      const isGroup = principal.type === 'GROUP'
      nodes.push({
        id: principal.principalId,
        position: { x: 0, y: 0 },
        data: {
          label: `${principal.principalId}${isGroup ? ' (group)' : ''}`,
          variant: PRINCIPAL_NODE_VARIANT[principal.type],
          isSelected,
        },
      })
    }

    for (const [groupId, memberIds] of Object.entries(graph.data.membership)) {
      for (const memberId of memberIds) {
        edges.push({
          id: `member:${groupId}->${memberId}`,
          source: groupId,
          target: memberId,
          label: 'member',
          style: { strokeDasharray: '4 2', stroke: MEMBER_EDGE_COLOR, strokeWidth: 2 },
          markerEnd: edgeMarker(MEMBER_EDGE_COLOR),
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
          data: { label: sourceId, variant: SOURCE_NODE_VARIANT },
        })
        for (const via of viaPrincipals) {
          edges.push({
            id: `grant:${via}->${sourceId}`,
            source: via,
            target: `source:${sourceId}`,
            label: 'grants',
            animated: true,
            style: { stroke: GRANT_EDGE_COLOR, strokeWidth: 2.5 },
            markerEnd: edgeMarker(GRANT_EDGE_COLOR),
          })
        }
      }
    }

    return { nodes, edges }
  }, [graph.data, permissions.data, selectedId])

  if (graph.isPending) return <Skeleton className="h-[420px] w-full rounded-xl" />
  if (graph.isError) return <ErrorState description={(graph.error as Error).message} />

  return (
    <GraphView
      nodes={nodes}
      edges={edges}
      onNodeClick={(id) => !id.startsWith('source:') && onSelect?.(id)}
    />
  )
}
