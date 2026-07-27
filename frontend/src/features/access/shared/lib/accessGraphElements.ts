import { MarkerType, type Edge, type Node } from '@xyflow/react'
import type { GraphNodeData, GraphNodeVariant } from '@/shared/components/ui/GraphView'
import { sourceNodeId } from './sourceNode'
import type { AccessGraphNodeKind } from '../types/access.type'

// Which variant a node *kind* gets — GraphView owns what each variant looks
// like, this only picks one per kind.
export const NODE_VARIANT: Record<AccessGraphNodeKind, GraphNodeVariant> = {
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
export type EdgeEmphasis = 'traced' | 'faded' | 'plain'

export function edgeMarker(color: string) {
  return { type: MarkerType.Arrow, color, width: EDGE_MARKER_SIZE, height: EDGE_MARKER_SIZE }
}

/** Applies the emphasis to a base edge look, shared by both edge kinds. */
export function emphasize(base: Edge, color: string, width: number, emphasis: EdgeEmphasis): Edge {
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

export function memberEdge(from: string, to: string, emphasis: EdgeEmphasis = 'plain'): Edge {
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

export function grantEdge(from: string, sourceId: string, emphasis: EdgeEmphasis = 'plain'): Edge {
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

export function principalNode(id: string, kind: AccessGraphNodeKind, isSelected: boolean): Node {
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

export function sourceNode(id: string): Node {
  return {
    id: sourceNodeId(id),
    position: { x: 0, y: 0 },
    data: { label: id, variant: NODE_VARIANT.SOURCE } satisfies GraphNodeData,
  }
}
