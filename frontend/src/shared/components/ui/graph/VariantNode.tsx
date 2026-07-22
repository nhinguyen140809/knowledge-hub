import { Handle, type NodeProps } from '@xyflow/react'
import { useGraphDimensions } from './GraphDimensionsContext'
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
 *  too; `sourcePosition`/`targetPosition` come from the dagre layout pass. */
export function VariantNode({ data, sourcePosition, targetPosition }: NodeProps) {
  const { nodeWidth, nodeHeight } = useGraphDimensions()
  const { label, variant = 'neutral', isSelected } = data as GraphNodeData
  const style = GRAPH_NODE_VARIANTS[variant]

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
        borderColor: style.borderColor,
        borderStyle: style.borderStyle,
        borderWidth: isSelected ? 2.5 : 1.5,
        boxShadow: isSelected ? `0 0 0 2px ${style.borderColor}` : undefined,
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
