import { Position } from '@xyflow/react'

/** Which sides edges leave and enter a node, given the layer direction. Shared
 *  by every layout engine so they can't drift into disagreeing about it — the
 *  arrowheads are drawn from these, so a mismatch would point edges the wrong
 *  way. Vertical layouts flow top→bottom, horizontal ones left→right. */
export function handlePositions(direction: 'TB' | 'LR'): {
  sourcePosition: Position
  targetPosition: Position
} {
  return direction === 'LR'
    ? { sourcePosition: Position.Right, targetPosition: Position.Left }
    : { sourcePosition: Position.Bottom, targetPosition: Position.Top }
}
