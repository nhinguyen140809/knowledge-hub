import { createContext, useContext } from 'react'

interface GraphDimensions {
  nodeWidth: number
  nodeHeight: number
}

const DEFAULT_DIMENSIONS: GraphDimensions = { nodeWidth: 172, nodeHeight: 44 }

/** VariantNode needs the node's own size to render at, but it's a
 *  `nodeTypes` entry — React Flow instantiates it, so GraphView can't hand
 *  the size down as a prop. Context is the one channel left; it also means
 *  `nodeTypes` stops being a value that depends on `nodeWidth`/`nodeHeight`,
 *  so it can be a plain module-level object instead of a `useMemo` keyed on
 *  props (previously: a factory rebuilt, and React Flow remounting every
 *  node, whenever those props changed). */
export const GraphDimensionsContext = createContext<GraphDimensions>(DEFAULT_DIMENSIONS)

export function useGraphDimensions() {
  return useContext(GraphDimensionsContext)
}
