import dagre from '@dagrejs/dagre'
import { Background, Controls, Position, ReactFlow, type Edge, type Node } from '@xyflow/react'
import '@xyflow/react/dist/style.css'
import { useTheme } from 'next-themes'
import { useMemo } from 'react'

/**
 * App-agnostic node-link view. React Flow ships no layout of its own, so nodes
 * are handed in *without* positions and dagre assigns them here — callers only
 * describe what connects to what. Holds no data and no domain knowledge.
 *
 * React Flow measures its container, so the wrapper needs a real height.
 */
export interface GraphViewProps {
  /** Positions are computed here; anything passed in is overwritten. */
  nodes: Node[]
  edges: Edge[]
  /** Top-to-bottom or left-to-right layering. */
  direction?: 'TB' | 'LR'
  nodeWidth?: number
  nodeHeight?: number
  className?: string
  onNodeClick?: (id: string) => void
}

function layout(
  nodes: Node[],
  edges: Edge[],
  direction: 'TB' | 'LR',
  nodeWidth: number,
  nodeHeight: number,
): Node[] {
  const graph = new dagre.graphlib.Graph()
  graph.setGraph({ rankdir: direction, nodesep: 24, ranksep: 56 })
  graph.setDefaultEdgeLabel(() => ({}))

  for (const node of nodes) graph.setNode(node.id, { width: nodeWidth, height: nodeHeight })
  // A cycle would make dagre loop, so edges that point back at a node already
  // linked in the other direction are dropped from the layout pass only.
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
      // dagre returns the node's centre; React Flow wants its top-left corner.
      position: { x: x - nodeWidth / 2, y: y - nodeHeight / 2 },
      sourcePosition: direction === 'LR' ? Position.Right : Position.Bottom,
      targetPosition: direction === 'LR' ? Position.Left : Position.Top,
    }
  })
}

export function GraphView({
  nodes,
  edges,
  direction = 'TB',
  nodeWidth = 172,
  nodeHeight = 44,
  className = 'h-[420px]',
  onNodeClick,
}: GraphViewProps) {
  const { resolvedTheme } = useTheme()
  const positioned = useMemo(
    () => layout(nodes, edges, direction, nodeWidth, nodeHeight),
    [nodes, edges, direction, nodeWidth, nodeHeight],
  )

  return (
    <div className={`w-full overflow-hidden rounded-xl border ${className}`}>
      <ReactFlow
        nodes={positioned}
        edges={edges}
        colorMode={resolvedTheme === 'dark' ? 'dark' : 'light'}
        fitView
        proOptions={{ hideAttribution: false }}
        nodesDraggable={false}
        nodesConnectable={false}
        onNodeClick={(_, node) => onNodeClick?.(node.id)}
      >
        <Background />
        <Controls showInteractive={false} />
      </ReactFlow>
    </div>
  )
}
