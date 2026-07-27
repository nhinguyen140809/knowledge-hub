import { useState } from 'react'
import { useLocation } from 'react-router-dom'

/** The right side is two views of the selected source. */
export type SourceAccessView = 'principals' | 'graph'

export interface SourceAccessSelection {
  selectedId: string | null
  view: SourceAccessView
  setView: (view: SourceAccessView) => void
  /** Select a source, or deselect it when it is already selected. */
  toggleSelect: (id: string) => void
}

/**
 * The Access-Control-Sources page's interaction state: which source is
 * selected, and which view (principals/graph) is showing. Simpler than
 * {@link ../../principal/hooks/usePrincipalAccessSelection}'s principal-side
 * counterpart — no trace state, since there's no source-to-source path to
 * highlight here. Also honours a selection carried in navigation state (e.g.
 * from a source's detail page), selecting that source on arrival.
 */
export function useSourceAccessSelection(): SourceAccessSelection {
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [view, setView] = useState<SourceAccessView>('principals')

  // Same render-time pattern as usePrincipalAccessSelection: navigation state
  // rather than a URL param, so opening the same source twice still
  // re-selects it (every navigation gets a fresh location.key).
  const location = useLocation()
  const requested = (location.state as { selectSource?: string } | null)?.selectSource ?? null
  const [prevKey, setPrevKey] = useState<string | null>(null)
  if (location.key !== prevKey) {
    setPrevKey(location.key)
    if (requested) setSelectedId(requested)
  }

  const toggleSelect = (id: string) => {
    setSelectedId((prev) => (prev === id ? null : id))
  }

  return { selectedId, view, setView, toggleSelect }
}
