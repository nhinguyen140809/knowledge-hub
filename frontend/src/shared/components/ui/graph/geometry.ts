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

/** A quadratic curve bowed a fixed fraction of its own length, always to the
 *  same side of travel — not React Flow's `getBezierPath`, whose control
 *  points are locked to whichever cardinal side (Top/Right/Bottom/Left) the
 *  endpoint is classified as. That lock is what breaks "arrow follows the
 *  body": it forces the curve to enter axis-aligned at the very end no
 *  matter how diagonal the line looks overall, and the arrowhead (correctly)
 *  follows that locked tangent, not the visible line. A curve with no
 *  cardinal lock has no such mismatch — the endpoint tangent stays close to
 *  the straight source→target direction, by an amount `bow` controls. */
export function getCurvedPath(sourceX: number, sourceY: number, targetX: number, targetY: number) {
  const bow = 0.15
  const dx = targetX - sourceX
  const dy = targetY - sourceY
  // Rotating (dx, dy) by 90° gives (-dy, dx) — a perpendicular offset scaled
  // to `bow` fraction of the line's own length. A fixed rotation direction
  // keeps every edge bowing the same way rather than flipping unpredictably
  // from edge to edge.
  const controlX = (sourceX + targetX) / 2 - dy * bow
  const controlY = (sourceY + targetY) / 2 + dx * bow

  return {
    path: `M${sourceX},${sourceY} Q${controlX},${controlY} ${targetX},${targetY}`,
    // Quadratic bezier position at t=0.5.
    labelX: 0.25 * sourceX + 0.5 * controlX + 0.25 * targetX,
    labelY: 0.25 * sourceY + 0.5 * controlY + 0.25 * targetY,
  }
}
