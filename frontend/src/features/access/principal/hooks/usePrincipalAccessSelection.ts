import { useState } from 'react'
import { useLocation } from 'react-router-dom'
import type { EffectiveSource } from '../types/principal.type'

/** The right side is two views of the selected principal; a click may need to
 *  steer which one is showing. */
export type PrincipalAccessView = 'details' | 'graph'

export interface PrincipalAccessSelection {
  selectedId: string | null
  view: PrincipalAccessView
  /** Source whose access path is being traced in the graph, or null for none.
   *  The graph derives which nodes and edges to light from this. */
  tracedSourceId: string | null
  setView: (view: PrincipalAccessView) => void
  /** Select a principal, or deselect it when it is already selected. */
  toggleSelect: (id: string) => void
  /** Drop the selection if the given principal was the selected one — for a
   *  delete that removes the currently shown principal. */
  clearIfSelected: (id: string) => void
  /** "Why can it read this": trace the clicked source's path, then switch to
   *  the graph so the lit path is visible. */
  traceAccess: (source: EffectiveSource) => void
}

/**
 * All of the Access-Control-Principals page's interaction state in one place:
 * which principal is selected, which view (details/graph) is showing, and any
 * access path being traced. Also honours a selection carried in navigation
 * state (e.g. from the command palette), selecting that principal on arrival.
 */
export function usePrincipalAccessSelection(): PrincipalAccessSelection {
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [view, setView] = useState<PrincipalAccessView>('details')
  // A path belongs to one principal's view, so it is cleared whenever the
  // selection changes.
  const [tracedSourceId, setTracedSourceId] = useState<string | null>(null)

  // Selecting from elsewhere arrives as navigation state, not a URL param, so
  // opening the same principal twice still re-selects it: every navigation gets
  // a fresh location.key even when the target is identical. Handled in render
  // (not an effect) by comparing the previous key — when the entry changes and
  // names a principal, select it.
  const location = useLocation()
  const requested = (location.state as { selectPrincipal?: string } | null)?.selectPrincipal ?? null
  const [prevKey, setPrevKey] = useState<string | null>(null)
  if (location.key !== prevKey) {
    setPrevKey(location.key)
    if (requested) {
      setSelectedId(requested)
      setTracedSourceId(null)
    }
  }

  const toggleSelect = (id: string) => {
    setTracedSourceId(null)
    setSelectedId((prev) => (prev === id ? null : id))
  }

  const clearIfSelected = (id: string) => {
    if (id === selectedId) {
      setSelectedId(null)
      setTracedSourceId(null)
    }
  }

  const traceAccess = (source: EffectiveSource) => {
    if (!selectedId) return
    setTracedSourceId(source.sourceId)
    setView('graph')
  }

  return { selectedId, view, tracedSourceId, setView, toggleSelect, clearIfSelected, traceAccess }
}
