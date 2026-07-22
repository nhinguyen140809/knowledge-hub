import ELK, { type ElkNode } from 'elkjs/lib/elk.bundled.js'
import { Position, type Edge, type Node } from '@xyflow/react'
import { useEffect, useState } from 'react'
import { VARIANT_NODE_TYPE } from './registry'

// A single shared instance — elk.layout() takes an input graph per call, it
// doesn't need to be recreated per layout pass.
const elk = new ELK()

/** Same contract as the old dagre-based layout (nodes in without positions,
 *  positioned nodes out), but elk's `layout()` is asynchronous — it runs off
 *  the main thread — so this returns the *previous* layout until the new one
 *  resolves, rather than blocking the render. Unlike dagre, elk's layered
 *  algorithm breaks cycles itself, so callers don't need to pre-filter
 *  back-edges before handing the graph over. */
export function useElkLayout(
  nodes: Node[],
  edges: Edge[],
  direction: 'TB' | 'LR',
  nodeWidth: number,
  nodeHeight: number,
): Node[] {
  const [positioned, setPositioned] = useState<Node[]>([])

  useEffect(() => {
    let cancelled = false

    elk
      .layout({
        id: 'root',
        layoutOptions: {
          'elk.algorithm': 'layered',
          'elk.direction': direction === 'LR' ? 'RIGHT' : 'DOWN',
          'elk.spacing.nodeNode': '24',
          'elk.layered.spacing.nodeNodeBetweenLayers': '56',
        },
        children: nodes.map((node) => ({ id: node.id, width: nodeWidth, height: nodeHeight })),
        edges: edges.map((edge) => ({
          id: edge.id,
          sources: [edge.source],
          targets: [edge.target],
        })),
      })
      .then((result: ElkNode) => {
        if (cancelled) return
        const positionById = new Map((result.children ?? []).map((child) => [child.id, child]))

        setPositioned(
          nodes.map((node) => {
            const placed = positionById.get(node.id)
            return {
              ...node,
              type: VARIANT_NODE_TYPE,
              position: { x: placed?.x ?? 0, y: placed?.y ?? 0 },
              sourcePosition: direction === 'LR' ? Position.Right : Position.Bottom,
              targetPosition: direction === 'LR' ? Position.Left : Position.Top,
            }
          }),
        )
      })

    return () => {
      cancelled = true
    }
  }, [nodes, edges, direction, nodeWidth, nodeHeight])

  return positioned
}
