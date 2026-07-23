import { Handle, type NodeProps } from '@xyflow/react'
import { useGraphDimensions } from './GraphDimensionsContext'
import { useGraphHighlight } from './GraphHighlightContext'
import { GRAPH_NODE_VARIANTS, type GraphNodeData } from './variants'

/** Handles still need to exist in the DOM (React Flow tracks node/edge
 *  attachment through them), but FloatingEdge computes its own attachment
 *  points from real node geometry and never reads a Handle's position — so
 *  the dot it renders by default has nothing left to signal and is just
 *  hidden. */
const HIDDEN_HANDLE_STYLE = { opacity: 0, pointerEvents: 'none' as const }

/** Renders a node purely from `data.variant` — colors and border style are
 *  never written at the call site. Handles are drawn by hand because
 *  replacing React Flow's default node type opts out of its automatic ones
 *  too; `sourcePosition`/`targetPosition` come from the layout pass.
 *
 *  Selection is the union of three signals: `data.isSelected` (the caller's
 *  app-level selection, e.g. synced with a tree), React Flow's own
 *  `selected` (set the instant a node is clicked, before any app state makes
 *  the round trip back down), and the highlight context (this node is an
 *  endpoint of the currently selected edge). Its look is deliberately
 *  variant-independent — an accent ring reads as "selected" on every
 *  variant, whereas ringing with the variant's own border color made
 *  selection invisible on the ones with muted borders. */
export function VariantNode({ id, data, selected, sourcePosition, targetPosition }: NodeProps) {
  const { nodeWidth, nodeHeight } = useGraphDimensions()
  const highlighted = useGraphHighlight()
  const { label, variant = 'neutral', isSelected } = data as GraphNodeData
  const style = GRAPH_NODE_VARIANTS[variant]
  const showSelected = isSelected || selected || highlighted.has(id)

  return (
    <div
      style={{
        width: nodeWidth,
        height: nodeHeight,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        overflow: 'hidden',
        textOverflow: 'ellipsis',
        whiteSpace: 'nowrap',
        borderRadius: 10,
        fontSize: 12,
        padding: 6,
        background: style.background,
        color: style.color,
        borderColor: showSelected ? 'var(--accent)' : style.borderColor,
        borderStyle: showSelected ? 'solid' : style.borderStyle,
        borderWidth: showSelected ? 2 : 1.5,
        boxShadow: showSelected ? '0 0 0 2.5px var(--accent)' : undefined,
      }}
    >
      {targetPosition && (
        <Handle type="target" position={targetPosition} style={HIDDEN_HANDLE_STYLE} />
      )}
      {label}
      {sourcePosition && (
        <Handle type="source" position={sourcePosition} style={HIDDEN_HANDLE_STYLE} />
      )}
    </div>
  )
}
