import dagre from '@dagrejs/dagre'
import { Position, type Edge, type Node } from '@xyflow/react'
import { useMemo } from 'react'
import { VARIANT_NODE_TYPE } from './registry'

/** Assigns every node a position via dagre, plus the node type and
 *  `sourcePosition`/`targetPosition` React Flow needs to render it. Nodes
 *  come in without positions — callers only describe what connects to what. */
export function useDagreLayout(
  nodes: Node[],
  edges: Edge[],
  direction: 'TB' | 'LR',
  nodeWidth: number,
  nodeHeight: number,
): Node[] {
  return useMemo(() => {
    const graph = new dagre.graphlib.Graph()
    graph.setGraph({ rankdir: direction, nodesep: 24, ranksep: 56 })
    graph.setDefaultEdgeLabel(() => ({}))

    for (const node of nodes) graph.setNode(node.id, { width: nodeWidth, height: nodeHeight })
    // A cycle would make dagre loop, so edges that point back at a node
    // already linked in the other direction are dropped from the layout
    // pass only.
    const seen = new Set<string>()
    for (const edge of edges) {
      if (seen.has(`${edge.target}->${edge.source}`)) continue
      seen.add(`${edge.source}->${edge.target}`)
      graph.setEdge(edge.source, edge.target)
    }

    dagre.layout(graph)

    return nodes.map((node) => {
      const { x, y } = graph.node(node.id)
      return {
        ...node,
        type: VARIANT_NODE_TYPE,
        // dagre returns the node's centre; React Flow wants its top-left corner.
        position: { x: x - nodeWidth / 2, y: y - nodeHeight / 2 },
        sourcePosition: direction === 'LR' ? Position.Right : Position.Bottom,
        targetPosition: direction === 'LR' ? Position.Left : Position.Top,
      }
    })
  }, [nodes, edges, direction, nodeWidth, nodeHeight])
}
