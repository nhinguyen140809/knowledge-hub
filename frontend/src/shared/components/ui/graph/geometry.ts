import type { useInternalNode } from '@xyflow/react'

export type InternalGraphNode = NonNullable<ReturnType<typeof useInternalNode>>

/** Where the straight line from `target`'s center crosses `node`'s
 *  rectangle — the real point an edge should touch, not whichever side a
 *  fixed handle happens to be on. Standard "floating edges" geometry:
 *  https://reactflow.dev/examples/edges/floating-edges */
export function getBoundaryIntersection(node: InternalGraphNode, other: InternalGraphNode) {
  const { x: nx, y: ny } = node.internals.positionAbsolute
  const { x: ox, y: oy } = other.internals.positionAbsolute
  const w = (node.measured.width ?? 0) / 2
  const h = (node.measured.height ?? 0) / 2
  const x2 = nx + w
  const y2 = ny + h
  const x1 = ox + (other.measured.width ?? 0) / 2
  const y1 = oy + (other.measured.height ?? 0) / 2

  const xx1 = (x1 - x2) / (2 * w) - (y1 - y2) / (2 * h)
  const yy1 = (x1 - x2) / (2 * w) + (y1 - y2) / (2 * h)
  const a = 1 / (Math.abs(xx1) + Math.abs(yy1) || 1)

  return { x: w * (a * xx1 + a * yy1) + x2, y: h * (-a * xx1 + a * yy1) + y2 }
}

/** How far (px) the S-curve's control points swing off the straight line.
 *  Grows with edge length but capped, so short edges flex gently and long
 *  edges don't turn into wild loops. */
const MAX_BOW = 24
const BOW_RATIO = 0.18

/** An S-shaped cubic bezier — the same silhouette React Flow's default
 *  bezier had, minus its flaw. `getBezierPath` locks each control point to
 *  whichever cardinal side (Top/Right/Bottom/Left) the endpoint is
 *  classified as, forcing the curve to enter axis-aligned at the very end no
 *  matter how diagonal the line is overall — and the arrowhead (correctly)
 *  follows that locked tangent, not the visible line. Here the two control
 *  points sit at 1/3 and 2/3 of the *actual* source→target line, pushed to
 *  opposite perpendicular sides: opposite sides are what make the S, and
 *  because they're anchored to the real line the endpoint tangents deviate
 *  from it only as far as the bow itself — so the arrowhead always follows
 *  the body it's drawn on. */
export function getCurvedPath(sourceX: number, sourceY: number, targetX: number, targetY: number) {
  const dx = targetX - sourceX
  const dy = targetY - sourceY
  const length = Math.hypot(dx, dy) || 1
  // Unit perpendicular: rotating the direction (dx, dy) by 90° gives
  // (-dy, dx). One fixed rotation keeps every edge swinging the same way.
  const perpX = -dy / length
  const perpY = dx / length
  const bow = Math.min(length * BOW_RATIO, MAX_BOW)

  const c1x = sourceX + dx / 3 + perpX * bow
  const c1y = sourceY + dy / 3 + perpY * bow
  const c2x = sourceX + (2 * dx) / 3 - perpX * bow
  const c2y = sourceY + (2 * dy) / 3 - perpY * bow

  return {
    path: `M${sourceX},${sourceY} C${c1x},${c1y} ${c2x},${c2y} ${targetX},${targetY}`,
    // Cubic bezier at t=0.5: (start + 3·c1 + 3·c2 + end) / 8. The opposite
    // perpendicular offsets cancel there, which parks the label exactly on
    // the straight midpoint — the calmest spot on an S-curve.
    labelX: (sourceX + 3 * c1x + 3 * c2x + targetX) / 8,
    labelY: (sourceY + 3 * c1y + 3 * c2y + targetY) / 8,
  }
}
