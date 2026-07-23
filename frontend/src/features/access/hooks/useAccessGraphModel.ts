import { MarkerType, type Edge, type Node } from '@xyflow/react'
import { useMemo } from 'react'
import type { GraphNodeData, GraphNodeVariant } from '@/shared/components/ui/GraphView'
import { useAccessGraph, usePrincipalGraph } from './usePrincipals'
import type { AccessGraphNodeKind } from '../types/access.type'

// Which variant a node *kind* gets — GraphView owns what each variant looks
// like, this only picks one per kind.
const NODE_VARIANT: Record<AccessGraphNodeKind, GraphNodeVariant> = {
  GROUP: 'accent',
  SUBJECT: 'neutral',
  SOURCE: 'success',
}

// Membership (structural, "belongs to") and grants (access, "can read") are
// the two kinds of edge in this graph — distinct color and marker size make
// that difference legible at a glance instead of only in the hover label.
const MEMBER_EDGE_COLOR = 'var(--muted)'
const GRANT_EDGE_COLOR = 'var(--success)'
const EDGE_MARKER_SIZE = 12

// Source ids live in a different namespace than principal ids and the two
// could collide; the prefix also lets the click handler tell them apart.
const SOURCE_PREFIX = 'source:'

/** Whether a graph node id names a source (as opposed to a principal). */
export function isSourceNodeId(id: string): boolean {
  return id.startsWith(SOURCE_PREFIX)
}

function edgeMarker(color: string) {
  return { type: MarkerType.Arrow, color, width: EDGE_MARKER_SIZE, height: EDGE_MARKER_SIZE }
}

function memberEdge(from: string, to: string): Edge {
  return {
    id: `member:${from}->${to}`,
    source: from,
    target: to,
    // Reads in the arrow's direction: group → "has member" → principal.
    label: 'has member',
    style: { stroke: MEMBER_EDGE_COLOR, strokeWidth: 2 },
    markerEnd: edgeMarker(MEMBER_EDGE_COLOR),
  }
}

function grantEdge(from: string, sourceId: string): Edge {
  return {
    id: `grant:${from}->${sourceId}`,
    source: from,
    target: `${SOURCE_PREFIX}${sourceId}`,
    // Reads in the arrow's direction: principal → "can read" → source.
    label: 'can read',
    style: { stroke: GRANT_EDGE_COLOR, strokeWidth: 2.5 },
    markerEnd: edgeMarker(GRANT_EDGE_COLOR),
  }
}

function principalNode(id: string, kind: AccessGraphNodeKind, isSelected: boolean): Node {
  return {
    id,
    position: { x: 0, y: 0 },
    data: {
      label: `${id}${kind === 'GROUP' ? ' (group)' : ''}`,
      variant: NODE_VARIANT[kind],
      isSelected,
    } satisfies GraphNodeData,
  }
}

function sourceNode(id: string): Node {
  return {
    id: `${SOURCE_PREFIX}${id}`,
    position: { x: 0, y: 0 },
    data: { label: id, variant: NODE_VARIANT.SOURCE } satisfies GraphNodeData,
  }
}

/**
 * Nodes and edges for the access graph's hybrid view: with nothing selected,
 * the whole membership graph as an overview; with a principal selected, the
 * scoped access-graph. The response decides what belongs in the subgraph;
 * this hook only translates its structure into styled elements.
 */
export function useAccessGraphModel(selectedId: string | null | undefined) {
  const overview = usePrincipalGraph()
  const scoped = useAccessGraph(selectedId ?? undefined)

  const { nodes, edges } = useMemo(() => {
    const nodes: Node[] = []
    const edges: Edge[] = []

    if (selectedId && scoped.data) {
      for (const node of scoped.data.nodes) {
        if (node.kind === 'SOURCE') nodes.push(sourceNode(node.id))
        else nodes.push(principalNode(node.id, node.kind, node.id === selectedId))
      }

      for (const edge of scoped.data.edges) {
        if (edge.kind === 'MEMBER') edges.push(memberEdge(edge.from, edge.to))
        else edges.push(grantEdge(edge.from, edge.to))
      }

      return { nodes, edges }
    }

    if (!overview.data) return { nodes, edges }
    for (const principal of overview.data.principals) {
      nodes.push(principalNode(principal.principalId, principal.type, false))
    }

    for (const [groupId, memberIds] of Object.entries(overview.data.membership)) {
      for (const memberId of memberIds) {
        edges.push(memberEdge(groupId, memberId))
      }
    }
    return { nodes, edges }
  }, [overview.data, scoped.data, selectedId])

  return {
    nodes,
    edges,
    isPending: selectedId ? scoped.isPending : overview.isPending,
    isError: selectedId ? scoped.isError : overview.isError,
    error: selectedId ? scoped.error : overview.error,
  }
}
