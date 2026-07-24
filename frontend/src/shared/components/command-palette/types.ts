import type { LucideIcon } from 'lucide-react'

/**
 * One entry in the command palette. Fully self-describing so the palette stays
 * domain-agnostic: whoever supplies the items owns the icon, the hint, and the
 * route it leads to. The palette never learns what a "principal" or "source" is.
 */
export interface CommandItem {
  /** Stable unique key across every item, regardless of source. */
  key: string
  /** Primary text shown, and part of what the query matches. */
  label: string
  /** Secondary tag shown at the row's end, e.g. the kind ("source", "group"). */
  hint?: string
  /** Lowercased text the typed query is matched against — usually the label
   *  plus any alternate name. */
  search: string
  /** Route the item navigates to when chosen. */
  to: string
  /** Leading glyph, picked by the supplier to distinguish kinds at a glance. */
  icon?: LucideIcon
}
