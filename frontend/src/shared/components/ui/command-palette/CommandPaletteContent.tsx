import { Kbd, SearchField, Separator } from '@heroui/react'
import { SearchX } from 'lucide-react'
import { type KeyboardEvent, useLayoutEffect, useMemo, useRef, useState } from 'react'
import { EmptyState } from '@/shared/components/ui/EmptyState'
import { matchesQuery } from './matchesQuery'
import { ResultRow } from './ResultRow'
import type { CommandItem } from './types'

/** Tallest the results box grows before it scrolls internally (px, matches h-80). */
const MAX_LIST_HEIGHT = 320

interface CommandPaletteContentProps {
  /** Supplies the items. Called here (not by the shell) so the items — and any
   *  queries behind them — are only produced while the palette is open. */
  useItems: () => CommandItem[]
  onClose: () => void
}

/** What fills the open palette: a search box over a filtered, keyboard-navigable
 *  list. Selecting a row runs its action and closes. Rendered only while the
 *  palette is open, so its data hooks stay idle until then. */
export function CommandPaletteContent({ useItems, onClose }: CommandPaletteContentProps) {
  const items = useItems()
  const [query, setQuery] = useState('')
  const [active, setActive] = useState(0)

  const results = useMemo(() => {
    const needle = query.trim().toLowerCase()
    if (!needle) return items
    return items.filter((item) => matchesQuery(needle, item.search))
  }, [items, query])

  // A fresh query re-ranks the list, so the highlight returns to the top. Done
  // in render (not an effect) by comparing the previous query — no extra render
  // pass, and the reset is applied before the new list ever paints.
  const [prevQuery, setPrevQuery] = useState(query)
  if (query !== prevQuery) {
    setPrevQuery(query)
    setActive(0)
  }

  const contentRef = useRef<HTMLDivElement>(null)
  const [listHeight, setListHeight] = useState<number>()
  useLayoutEffect(() => {
    const el = contentRef.current
    if (el) setListHeight(Math.min(el.offsetHeight, MAX_LIST_HEIGHT))
  }, [results])

  function run(item: CommandItem | undefined) {
    if (!item) return
    item.action()
    onClose()
  }

  // Keyboard nav lives on the input itself: React Aria (under SearchField) stops
  // keydown from bubbling, so a handler on an ancestor never sees the arrows.
  // Enter runs the active row; the arrows move the highlight and wrap at
  // either end, so Down from the last row reaches the first and vice versa.
  function onKeyDown(e: KeyboardEvent) {
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      if (results.length > 0) setActive((i) => (i + 1) % results.length)
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      if (results.length > 0) setActive((i) => (i - 1 + results.length) % results.length)
    } else if (e.key === 'Enter') {
      e.preventDefault()
      run(results[active])
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <SearchField
        aria-label="Search"
        value={query}
        onChange={setQuery}
        autoFocus
        variant="secondary"
        fullWidth
      >
        <SearchField.Group>
          <SearchField.SearchIcon />
          <SearchField.Input placeholder="Jump to..." onKeyDown={onKeyDown} />
          <SearchField.ClearButton />
        </SearchField.Group>
      </SearchField>

      <div
        style={{ height: listHeight }}
        className="scrollbar-thin overflow-y-auto transition-[height] duration-200 ease-out"
      >
        <div ref={contentRef}>
          {results.length === 0 ? (
            <EmptyState
              icon={<SearchX size={28} />}
              description="No matches. Try a different search."
            />
          ) : (
            results.map((item, i) => (
              <ResultRow
                key={item.key}
                item={item}
                isActive={i === active}
                onActivate={() => setActive(i)}
                onRun={() => run(item)}
              />
            ))
          )}
        </div>
      </div>

      <Separator orientation="horizontal" />

      <div className="text-muted flex items-center gap-4 text-sm">
        <span className="flex items-center gap-1.5">
          <Kbd>
            <Kbd.Abbr keyValue="up" />
          </Kbd>
          <Kbd>
            <Kbd.Abbr keyValue="down" />
          </Kbd>
          navigate
        </span>
        <span className="flex items-center gap-1.5">
          <Kbd>Enter</Kbd>
          open
        </span>
        <span className="flex items-center gap-1.5">
          <Kbd>Esc</Kbd>
          close
        </span>
      </div>
    </div>
  )
}
