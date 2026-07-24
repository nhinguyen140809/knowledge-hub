import { MarkerType, type Edge, type Node } from '@xyflow/react'
import { useMemo } from 'react'
import type { GraphNodeData, GraphNodeVariant } from '@/shared/components/ui/GraphView'
import { sourceNodeId } from '../lib/sourceNode'
import { tracePath, type TracedPath } from '../lib/tracePath'
import { useAccessGraph, usePrincipalGraph } from './usePrincipals'
import type { AccessGraphEdge, AccessGraphNodeKind } from '../types/access.type'

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

const TRACE_EDGE_COLOR = 'var(--accent)'

// While a path is traced, edges are either on it (accent, thicker, on top) or
// off it (faded so the path reads clearly). 'plain' is the untraced default.
type EdgeEmphasis = 'traced' | 'faded' | 'plain'

function edgeMarker(color: string) {
  return { type: MarkerType.Arrow, color, width: EDGE_MARKER_SIZE, height: EDGE_MARKER_SIZE }
}

/** Applies the emphasis to a base edge look, shared by both edge kinds. */
function emphasize(base: Edge, color: string, width: number, emphasis: EdgeEmphasis): Edge {
  if (emphasis === 'traced') {
    return {
      ...base,
      style: { stroke: TRACE_EDGE_COLOR, strokeWidth: width + 1.5 },
      markerEnd: edgeMarker(TRACE_EDGE_COLOR),
      zIndex: 10,
    }
  }
  if (emphasis === 'faded') {
    // The arrowhead is a separate marker that path opacity can't fade, so a
    // faded edge simply drops it — the line alone reads as background.
    return { ...base, style: { stroke: color, strokeWidth: width, opacity: 0.15 } }
  }
  return { ...base, style: { stroke: color, strokeWidth: width }, markerEnd: edgeMarker(color) }
}

function memberEdge(from: string, to: string, emphasis: EdgeEmphasis = 'plain'): Edge {
  return emphasize(
    {
      id: `member:${from}->${to}`,
      source: from,
      target: to,
      // Reads in the arrow's direction: group → "has member" → principal.
      label: 'has member',
    },
    MEMBER_EDGE_COLOR,
    2,
    emphasis,
  )
}

function grantEdge(from: string, sourceId: string, emphasis: EdgeEmphasis = 'plain'): Edge {
  return emphasize(
    {
      id: `grant:${from}->${sourceId}`,
      source: from,
      target: sourceNodeId(sourceId),
      // Reads in the arrow's direction: principal → "can read" → source.
      label: 'can read',
    },
    GRANT_EDGE_COLOR,
    2.5,
    emphasis,
  )
}

/** Emphasis for one scoped edge given the traced path (null when not tracing). */
function edgeEmphasisFor(traced: TracedPath | null, edge: AccessGraphEdge): EdgeEmphasis {
  if (!traced) return 'plain'
  return traced.edgeKeys.has(`${edge.kind}:${edge.from}->${edge.to}`) ? 'traced' : 'faded'
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
    id: sourceNodeId(id),
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
export function useAccessGraphModel(
  selectedId: string | null | undefined,
  traceSourceId?: string | null,
) {
  const overview = usePrincipalGraph()
  const scoped = useAccessGraph(selectedId ?? undefined)

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
