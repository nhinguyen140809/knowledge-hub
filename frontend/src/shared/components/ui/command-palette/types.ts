import type { LucideIcon } from 'lucide-react'

/**
 * One entry in the command palette. Fully self-describing so the palette stays
 * domain-agnostic: whoever supplies the items owns the icon, the hint, and what
 * running it does. The palette never learns what a "principal" or "source" is,
 * nor how to navigate — it only renders and, on select, calls `action`.
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
  /** What running the item does (navigate, toggle, run a command, ...). The
   *  palette invokes this on select and then closes; it never inspects it. */
  action: () => void
  /** Leading glyph, picked by the supplier to distinguish kinds at a glance. */
  icon?: LucideIcon
}
