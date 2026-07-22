import { Background, Controls, MarkerType, ReactFlow, type Edge, type Node } from '@xyflow/react'
import '@xyflow/react/dist/style.css'
import { useTheme } from 'next-themes'
import { GraphDimensionsContext } from './graph/GraphDimensionsContext'
import { FLOATING_EDGE_TYPE, graphEdgeTypes, graphNodeTypes } from './graph/registry'
import { useDagreLayout } from './graph/useDagreLayout'
import { useElkLayout } from './graph/useElkLayout'

/**
 * App-agnostic node-link view. React Flow ships no layout of its own, so nodes
 * are handed in *without* positions and a layout engine assigns them here —
 * callers only describe what connects to what. Holds no data and no domain
 * knowledge beyond the node variant system (see `graph/variants.ts`).
 *
 * React Flow measures its container, so the wrapper needs a real height.
 */

export type { GraphNodeData, GraphNodeVariant } from './graph/variants'

export interface GraphViewProps {
  /** Positions are computed here; anything passed in is overwritten. `data`
   *  should follow {@link GraphNodeData} to get the variant styling. */
  nodes: Node[]
  edges: Edge[]
  /** Top-to-bottom or left-to-right layering. */
  direction?: 'TB' | 'LR'
  nodeWidth?: number
  nodeHeight?: number
  className?: string
  onNodeClick?: (id: string) => void
  /** @default 'elk' — elk's layered algorithm breaks cycles on its own and
   *  tends to route fewer overlaps; dagre stays available to compare against
   *  since it's synchronous (no layout-settling frame) and lighter-weight. */
  layoutEngine?: 'dagre' | 'elk'
}

export function GraphView({
  nodes,
  edges,
  direction = 'TB',
  nodeWidth = 172,
  nodeHeight = 44,
  className = 'h-[420px]',
  onNodeClick,
  layoutEngine = 'elk',
}: GraphViewProps) {
  const { resolvedTheme } = useTheme()
  // Both engines run unconditionally — hooks can't be called behind a
  // branch — and the unused one's result is simply not read.
  const dagrePositioned = useDagreLayout(nodes, edges, direction, nodeWidth, nodeHeight)
  const elkPositioned = useElkLayout(nodes, edges, direction, nodeWidth, nodeHeight)
  const positioned = layoutEngine === 'dagre' ? dagrePositioned : elkPositioned

  return (
    <GraphDimensionsContext.Provider value={{ nodeWidth, nodeHeight }}>
      <div className={`w-full overflow-hidden rounded-xl border ${className}`}>
        <ReactFlow
          nodes={positioned}
          edges={edges}
          nodeTypes={graphNodeTypes}
          edgeTypes={graphEdgeTypes}
          colorMode={resolvedTheme === 'dark' ? 'dark' : 'light'}
          fitView
          proOptions={{ hideAttribution: false }}
          nodesDraggable={false}
          nodesConnectable={false}
          defaultEdgeOptions={{
            type: FLOATING_EDGE_TYPE,
            markerEnd: { type: MarkerType.Arrow, width: 12, height: 12 },
          }}
          onNodeClick={(_, node) => onNodeClick?.(node.id)}
        >
          <Background />
          <Controls showInteractive={false} />
        </ReactFlow>
      </div>
    </GraphDimensionsContext.Provider>
  )
}
