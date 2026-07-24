import type { PrincipalAccessGraph } from '../types/access.type'

export interface TracedPath {
  /** Principal node ids on the path (holders, intermediate groups, focus). */
  principalIds: Set<string>
  /** Source node ids on the path (the traced source). */
  sourceIds: Set<string>
  /** `${kind}:${from}->${to}` for each edge on the path. */
  edgeKeys: Set<string>
}

const EMPTY: TracedPath = {
  principalIds: new Set(),
  sourceIds: new Set(),
  edgeKeys: new Set(),
}

function push(map: Map<string, string[]>, key: string, value: string) {
  const existing = map.get(key)
  if (existing) existing.push(value)
  else map.set(key, [value])
}

/** Every node reachable from `starts` by following `adjacency`, starts included. */
function reachable(adjacency: Map<string, string[]>, starts: Iterable<string>): Set<string> {
  const seen = new Set<string>(starts)
  const queue = [...seen]
  while (queue.length > 0) {
    const node = queue.shift()!
    for (const next of adjacency.get(node) ?? []) {
      if (!seen.has(next)) {
        seen.add(next)
        queue.push(next)
      }
    }
  }
  return seen
}

/**
 * The nodes and edges explaining why `focus` can read `sourceId` inside its
 * access graph: the grant edge(s) into the source plus the membership path(s)
 * from each grant holder down to focus, including every intermediate group.
 *
 * A node lies on the path when it is both a descendant of some grant holder and
 * an ancestor of focus (inclusive on both ends); an edge lies on the path when
 * its parent end is holder-reachable and its member end can still reach focus.
 * Two reachability sweeps over the membership edges settle both. Empty when the
 * source has no grant path (reached only by policy or admin role).
 */
export function tracePath(
  graph: PrincipalAccessGraph,
  focus: string,
  sourceId: string,
): TracedPath {
  const grantEdges = graph.edges.filter((e) => e.kind === 'GRANT' && e.to === sourceId)
  if (grantEdges.length === 0) return EMPTY

  const memberEdges = graph.edges.filter((e) => e.kind === 'MEMBER')
  const children = new Map<string, string[]>() // group -> its members (edge direction)
  const parents = new Map<string, string[]>() // member -> its groups (reversed)
  for (const e of memberEdges) {
    push(children, e.from, e.to)
    push(parents, e.to, e.from)
  }

  // Ancestors of focus (nodes from which focus is reachable) and descendants of
  // the grant holders. Their intersection is exactly the path's nodes.
  const ancestorsOfFocus = reachable(parents, [focus])
  const descendantsOfHolders = reachable(
    children,
    grantEdges.map((e) => e.from),
  )

  const principalIds = new Set<string>()
  const edgeKeys = new Set<string>()
  for (const id of descendantsOfHolders) {
    if (ancestorsOfFocus.has(id)) principalIds.add(id)
  }
  for (const e of memberEdges) {
    if (descendantsOfHolders.has(e.from) && ancestorsOfFocus.has(e.to)) {
      edgeKeys.add(`MEMBER:${e.from}->${e.to}`)
    }
  }
  for (const e of grantEdges) {
    if (ancestorsOfFocus.has(e.from)) {
      edgeKeys.add(`GRANT:${e.from}->${e.to}`)
      principalIds.add(e.from)
    }
  }

  return { principalIds, sourceIds: new Set([sourceId]), edgeKeys }
}
