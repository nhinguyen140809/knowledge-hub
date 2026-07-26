import type { Edge, Node } from '@xyflow/react'
import { useMemo } from 'react'
import { grantEdge, memberEdge, principalNode, sourceNode } from '../lib/accessGraphElements'
import { useSourceAccessGraph } from './useSourceAccess'

/**
 * Nodes and edges for one source's access graph: the source is always the
 * entry point here, so unlike the principal side there's no overview-when-
 * empty mode and no trace/emphasis — the scoped subgraph already is the
 * answer, not something narrowed from a bigger one. `selectedNodeId` only
 * rings the clicked principal (a local, presentation-only selection — there's
 * no page to re-focus the way clicking a principal does on the principal
 * side); edge selection and its endpoint highlight come from `GraphView`
 * itself.
 */
export function useSourceAccessGraphModel(
  sourceId: string | undefined,
  selectedNodeId?: string | null,
) {
  const graph = useSourceAccessGraph(sourceId)

  const { nodes, edges } = useMemo(() => {
    const nodes: Node[] = []
    const edges: Edge[] = []
    if (!graph.data) return { nodes, edges }

    for (const node of graph.data.nodes) {
      if (node.kind === 'SOURCE') nodes.push(sourceNode(node.id))
      else nodes.push(principalNode(node.id, node.kind, node.id === selectedNodeId))
    }
    for (const edge of graph.data.edges) {
      edges.push(
        edge.kind === 'MEMBER' ? memberEdge(edge.from, edge.to) : grantEdge(edge.from, edge.to),
      )
    }
    return { nodes, edges }
  }, [graph.data, selectedNodeId])

  return { nodes, edges, isPending: graph.isPending, isError: graph.isError, error: graph.error }
}
