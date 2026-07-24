import ELK, { type ElkNode } from 'elkjs/lib/elk.bundled.js'
import { type Edge, type Node } from '@xyflow/react'
import { useEffect, useState } from 'react'
import { handlePositions } from './layout'
import { VARIANT_NODE_TYPE } from './registry'

// A single shared instance — elk.layout() takes an input graph per call, it
// doesn't need to be recreated per layout pass.
const elk = new ELK()

/** `layered` ranks nodes into rows/columns along a direction; `force` is a
 *  physical simulation with no direction at all — connected nodes attract,
 *  everything else repels, and the graph settles into whatever shape its
 *  connectivity implies. */
export type ElkAlgorithm = 'layered' | 'force'

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
  algorithm: ElkAlgorithm = 'layered',
): Node[] {
  const [positioned, setPositioned] = useState<Node[]>([])

  useEffect(() => {
    let cancelled = false

    const layoutOptions: Record<string, string> =
      algorithm === 'force'
        ? {
            'elk.algorithm': 'force',
            // Force treats nodes as points, so the spacing has to account for
            // the box drawn around each point or boxes overlap.
            'elk.spacing.nodeNode': '80',
          }
        : {
            'elk.algorithm': 'layered',
            'elk.direction': direction === 'LR' ? 'RIGHT' : 'DOWN',
            'elk.spacing.nodeNode': '24',
            'elk.layered.spacing.nodeNodeBetweenLayers': '56',
          }

    elk
      .layout({
        id: 'root',
        layoutOptions,
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
              ...handlePositions(direction),
            }
          }),
        )
      })

    return () => {
      cancelled = true
    }
  }, [nodes, edges, direction, nodeWidth, nodeHeight, algorithm])

  return positioned
}
