import {
  Background,
  Controls,
  MarkerType,
  ReactFlow,
  useEdgesState,
  useNodesState,
  type Edge,
  type Node,
} from '@xyflow/react'
import '@xyflow/react/dist/style.css'
import { useTheme } from 'next-themes'
import { useEffect, useMemo } from 'react'
import { GraphDimensionsContext } from './graph/GraphDimensionsContext'
import { GraphHighlightContext } from './graph/GraphHighlightContext'
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
  /** 'TB'/'LR' layer the graph top-to-bottom or left-to-right; 'auto' lets a
   *  force simulation settle it with no direction at all (elk engine only —
   *  dagre has no equivalent and falls back to 'TB'). */
  direction?: 'TB' | 'LR' | 'auto'
  nodeWidth?: number
  nodeHeight?: number
  className?: string
  onNodeClick?: (id: string) => void
  /** Node ids to render ringed regardless of selection — lets a caller trace a
   *  path (its endpoints and waypoints) from outside the graph. Merges with the
   *  ring a clicked edge already puts on its two ends. */
  highlightNodeIds?: ReadonlySet<string>
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
  className = 'h-105',
  onNodeClick,
  highlightNodeIds,
  layoutEngine = 'elk',
}: GraphViewProps) {
  const { resolvedTheme } = useTheme()
  const layerDirection = direction === 'auto' ? 'TB' : direction
  // Both engines run unconditionally — hooks can't be called behind a
  // branch — and the unused one's result is simply not read.
  const dagrePositioned = useDagreLayout(nodes, edges, layerDirection, nodeWidth, nodeHeight)
  const elkPositioned = useElkLayout(
    nodes,
    edges,
    layerDirection,
    nodeWidth,
    nodeHeight,
    direction === 'auto' ? 'force' : 'layered',
  )
  const positioned = layoutEngine === 'dagre' ? dagrePositioned : elkPositioned

  // React Flow's own state must be the one rendered: passing arrays straight
  // through props is "controlled mode", where interactive changes — including
  // click-selection of nodes and edges — are only *emitted* through the
  // change callbacks and silently dropped unless something applies them.
  // These hooks hold the applied state; the effects re-seed it whenever the
  // caller's data or the layout actually changes.
  const [rfNodes, setRfNodes, onNodesChange] = useNodesState<Node>([])
  const [rfEdges, setRfEdges, onEdgesChange] = useEdgesState<Edge>([])
  useEffect(() => setRfNodes(positioned), [positioned, setRfNodes])
  useEffect(() => setRfEdges(edges), [edges, setRfEdges])

  // Two highlight sources merge here: a caller-supplied set (a traced path) and
  // the two ends of a selected edge — an edge is a relationship, and a
  // relationship is meaningless without its ends. With multi-select disabled
  // below, React Flow guarantees the "one edge OR one node at a time" rule, so
  // the edge part never overlaps a node selection.
  const highlightedNodeIds = useMemo<ReadonlySet<string>>(() => {
    const ids = new Set<string>(highlightNodeIds)
    const selectedEdge = rfEdges.find((edge) => edge.selected)
    if (selectedEdge) {
      ids.add(selectedEdge.source)
      ids.add(selectedEdge.target)
    }
    return ids
  }, [rfEdges, highlightNodeIds])

  return (
    <GraphDimensionsContext.Provider value={{ nodeWidth, nodeHeight }}>
      <GraphHighlightContext.Provider value={highlightedNodeIds}>
        <div className={`w-full overflow-hidden rounded-xl border ${className}`}>
          <ReactFlow
            nodes={rfNodes}
            edges={rfEdges}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            nodeTypes={graphNodeTypes}
            edgeTypes={graphEdgeTypes}
            colorMode={resolvedTheme === 'dark' ? 'dark' : 'light'}
            fitView
            proOptions={{ hideAttribution: false }}
            nodesDraggable={false}
            nodesConnectable={false}
            // Single selection only: no modifier-click accumulation, no
            // drag-a-box selection. A plain click already deselects
            // everything else, which is the whole selection model here.
            multiSelectionKeyCode={null}
            selectionKeyCode={null}
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
      </GraphHighlightContext.Provider>
    </GraphDimensionsContext.Provider>
  )
}
