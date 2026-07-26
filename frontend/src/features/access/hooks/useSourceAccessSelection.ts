import { useState } from 'react'

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
 * {@link ../hooks/useAccessSelection}'s principal-side counterpart — no trace
 * state, since there's no source-to-source path to highlight here.
 */
export function useSourceAccessSelection(): SourceAccessSelection {
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [view, setView] = useState<SourceAccessView>('principals')

  const toggleSelect = (id: string) => {
    setSelectedId((prev) => (prev === id ? null : id))
  }

  return { selectedId, view, setView, toggleSelect }
}
