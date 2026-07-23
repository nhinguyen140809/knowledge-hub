import { createContext, useContext } from 'react'

const EMPTY: ReadonlySet<string> = new Set()

/** Node ids that should render as selected even though the node itself isn't
 *  the selected element — used to light up a selected edge's two endpoints.
 *  Context instead of node `data` so GraphView doesn't have to rebuild every
 *  node object (and make React Flow reconcile them all) on each selection
 *  change. */
export const GraphHighlightContext = createContext<ReadonlySet<string>>(EMPTY)

export function useGraphHighlight() {
  return useContext(GraphHighlightContext)
}
