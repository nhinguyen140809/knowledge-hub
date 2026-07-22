import { BaseEdge, useInternalNode, type EdgeProps } from '@xyflow/react'
import { getBoundaryIntersection, getCurvedPath } from './geometry'

/** An edge whose endpoints — and therefore its entry angle, and therefore
 *  the arrowhead React Flow draws on top of it — are recomputed from where
 *  the two nodes actually sit, instead of a fixed side declared once on the
 *  node. Keeps a smooth curve while letting the arrowhead genuinely point
 *  the way the line is going. */
export function FloatingEdge({
  id,
  source,
  target,
  selected,
  markerStart,
  markerEnd,
  style,
  label,
  labelStyle,
  labelShowBg,
  labelBgStyle,
  labelBgPadding,
  labelBgBorderRadius,
  interactionWidth,
}: EdgeProps) {
  const sourceNode = useInternalNode(source)
  const targetNode = useInternalNode(target)
  if (!sourceNode || !targetNode) return null

  const sourcePoint = getBoundaryIntersection(sourceNode, targetNode)
  const targetPoint = getBoundaryIntersection(targetNode, sourceNode)
  const { path, labelX, labelY } = getCurvedPath(
    sourcePoint.x,
    sourcePoint.y,
    targetPoint.x,
    targetPoint.y,
  )
  // Every edge sets its own `stroke` inline (see AccessGraph's member/grant
  // colors), which — being inline — always wins over React Flow's own CSS
  // rule for `.selected`. Without this, clicking an edge does register (it's
  // just as clickable as any built-in edge type) but produces no visible
  // change at all, which reads as "not clickable".
  const currentWidth = typeof style?.strokeWidth === 'number' ? style.strokeWidth : 2
  const edgeStyle = selected
    ? { ...style, stroke: 'var(--accent)', strokeWidth: currentWidth + 1 }
    : style

  return (
    <BaseEdge
      id={id}
      path={path}
      labelX={labelX}
      labelY={labelY}
      label={label}
      labelStyle={labelStyle}
      labelShowBg={labelShowBg}
      labelBgStyle={labelBgStyle}
      labelBgPadding={labelBgPadding}
      labelBgBorderRadius={labelBgBorderRadius}
      markerStart={markerStart}
      markerEnd={markerEnd}
      style={edgeStyle}
      interactionWidth={interactionWidth}
    />
  )
}
