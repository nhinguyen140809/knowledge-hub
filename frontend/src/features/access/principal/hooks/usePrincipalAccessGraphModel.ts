import { type Edge, type Node } from '@xyflow/react'
import { useMemo } from 'react'
import {
  grantEdge,
  memberEdge,
  principalNode,
  sourceNode,
  type EdgeEmphasis,
} from '../../shared/lib/accessGraphElements'
import { sourceNodeId } from '../../shared/lib/sourceNode'
import type { AccessGraphEdge } from '../../shared/types/access.type'
import { tracePath, type TracedPath } from '../lib/tracePath'
import { usePrincipalAccessGraph, usePrincipalGraph } from './usePrincipals'

/** Emphasis for one scoped edge given the traced path (null when not tracing). */
function edgeEmphasisFor(traced: TracedPath | null, edge: AccessGraphEdge): EdgeEmphasis {
  if (!traced) return 'plain'
  return traced.edgeKeys.has(`${edge.kind}:${edge.from}->${edge.to}`) ? 'traced' : 'faded'
}

/**
 * Nodes and edges for the access graph's hybrid view: with nothing selected,
 * the whole membership graph as an overview; with a principal selected, the
 * scoped access-graph. The response decides what belongs in the subgraph;
 * this hook only translates its structure into styled elements.
 */
export function usePrincipalAccessGraphModel(
  selectedId: string | null | undefined,
  traceSourceId?: string | null,
) {
  const overview = usePrincipalGraph()
  const scoped = usePrincipalAccessGraph(selectedId ?? undefined)

  const { nodes, edges, highlightNodeIds } = useMemo(() => {
    const nodes: Node[] = []
    const edges: Edge[] = []

    if (selectedId && scoped.data) {
      // The path from focus to the traced source, computed from the scoped
      // graph we already hold — no round trip, and guaranteed to reference only
      // edges this graph actually draws.
      const traced =
        traceSourceId != null ? tracePath(scoped.data, selectedId, traceSourceId) : null

      for (const node of scoped.data.nodes) {
        if (node.kind === 'SOURCE') nodes.push(sourceNode(node.id))
        else nodes.push(principalNode(node.id, node.kind, node.id === selectedId))
      }

      for (const edge of scoped.data.edges) {
        const emphasis = edgeEmphasisFor(traced, edge)
        if (edge.kind === 'MEMBER') edges.push(memberEdge(edge.from, edge.to, emphasis))
        else edges.push(grantEdge(edge.from, edge.to, emphasis))
      }

      const highlightNodeIds = traced
        ? new Set<string>([...traced.principalIds, ...[...traced.sourceIds].map(sourceNodeId)])
        : null
      return { nodes, edges, highlightNodeIds }
    }

    if (!overview.data) return { nodes, edges, highlightNodeIds: null }
    for (const principal of overview.data.principals) {
      nodes.push(principalNode(principal.principalId, principal.type, false))
    }

    for (const [groupId, memberIds] of Object.entries(overview.data.membership)) {
      for (const memberId of memberIds) {
        edges.push(memberEdge(groupId, memberId))
      }
    }
    return { nodes, edges, highlightNodeIds: null }
  }, [overview.data, scoped.data, selectedId, traceSourceId])

  return {
    nodes,
    edges,
    highlightNodeIds,
    isPending: selectedId ? scoped.isPending : overview.isPending,
    isError: selectedId ? scoped.isError : overview.isError,
    error: selectedId ? scoped.error : overview.error,
  }
}
