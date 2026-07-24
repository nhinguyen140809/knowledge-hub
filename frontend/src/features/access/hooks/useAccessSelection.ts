import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import type { EffectiveSource } from '../types/access.type'

/** The right side is two views of the selected principal; a click may need to
 *  steer which one is showing. */
export type AccessView = 'details' | 'graph'

export interface AccessSelection {
  selectedId: string | null
  view: AccessView
  /** Source whose access path is being traced in the graph, or null for none.
   *  The graph derives which nodes and edges to light from this. */
  tracedSourceId: string | null
  setView: (view: AccessView) => void
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
 * All of the Access page's interaction state in one place: which principal is
 * selected, which view (details/graph) is showing, and any access path being
 * traced. Also honours a `?principal=<id>` deep link (e.g. from the command
 * palette), selecting that principal when the URL names one.
 */
export function useAccessSelection(): AccessSelection {
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [view, setView] = useState<AccessView>('details')
  // A path belongs to one principal's view, so it is cleared whenever the
  // selection changes.
  const [tracedSourceId, setTracedSourceId] = useState<string | null>(null)

  // Deep link: selecting the principal named by ?principal=<id>. Compared in
  // render against the previous value (not in an effect) so a param change
  // applies before the next paint without an extra render pass. Read-only —
  // in-page selection never writes the param back.
  const [searchParams] = useSearchParams()
  const linkedPrincipal = searchParams.get('principal')
  const [prevLinked, setPrevLinked] = useState<string | null>(null)
  if (linkedPrincipal && linkedPrincipal !== prevLinked) {
    setPrevLinked(linkedPrincipal)
    setSelectedId(linkedPrincipal)
    setTracedSourceId(null)
  }

  const toggleSelect = (id: string) => {
    setTracedSourceId(null)
    setSelectedId((prev) => (prev === id ? null : id))
  }

  const clearIfSelected = (id: string) => {
    if (id === selectedId) setSelectedId(null)
  }

  const traceAccess = (source: EffectiveSource) => {
    if (!selectedId) return
    setTracedSourceId(source.sourceId)
    setView('graph')
  }

  return { selectedId, view, tracedSourceId, setView, toggleSelect, clearIfSelected, traceAccess }
}
