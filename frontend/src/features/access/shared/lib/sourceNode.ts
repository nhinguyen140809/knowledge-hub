// Source ids live in a different namespace than principal ids and the two could
// collide, so source nodes in the access graph carry a prefix; it also lets a
// click handler tell a source node from a principal node. Kept in its own
// module — free of any graph-library import — so page code that only needs to
// name a source node can do so without pulling React Flow into its bundle.
const SOURCE_PREFIX = 'source:'

/** Whether a graph node id names a source (as opposed to a principal). */
export function isSourceNodeId(id: string): boolean {
  return id.startsWith(SOURCE_PREFIX)
}

/** The graph node id for a source, the inverse of {@link isSourceNodeId}. */
export function sourceNodeId(id: string): string {
  return `${SOURCE_PREFIX}${id}`
}
