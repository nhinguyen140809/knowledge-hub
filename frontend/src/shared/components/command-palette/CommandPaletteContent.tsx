import { Kbd, ScrollShadow, SearchField, Separator } from '@heroui/react'
import { type KeyboardEvent, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { matchesQuery } from './matchesQuery'
import { ResultRow } from './ResultRow'
import type { CommandItem } from './types'

interface CommandPaletteContentProps {
  /** Supplies the items. Called here (not by the shell) so the items — and any
   *  queries behind them — are only produced while the palette is open. */
  useItems: () => CommandItem[]
  onClose: () => void
}

/** What fills the open palette: a search box over a filtered, keyboard-navigable
 *  list. Selecting a row navigates to its route and closes. Rendered only while
 *  the palette is open, so its data hooks stay idle until then. */
export function CommandPaletteContent({ useItems, onClose }: CommandPaletteContentProps) {
  const navigate = useNavigate()
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

  function run(item: CommandItem | undefined) {
    if (!item) return
    navigate(item.to)
    onClose()
  }

  // Arrow keys move the highlight; Enter is handled by SearchField's onSubmit.
  function onKeyDown(e: KeyboardEvent) {
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      setActive((i) => Math.min(results.length - 1, i + 1))
    } else if (e.key === 'ArrowUp') {
      e.preventDefault()
      setActive((i) => Math.max(0, i - 1))
    }
  }

  return (
    <div onKeyDown={onKeyDown} className="flex flex-col gap-4">
      <SearchField
        aria-label="Search"
        value={query}
        onChange={setQuery}
        onSubmit={() => run(results[active])}
        autoFocus
        variant="secondary"
        fullWidth
      >
        <SearchField.Group>
          <SearchField.SearchIcon />
          <SearchField.Input placeholder="Jump to..." />
          <SearchField.ClearButton />
        </SearchField.Group>
      </SearchField>

      <ScrollShadow className="max-h-80" offset={2}>
        <div>
          {results.length === 0 ? (
            <p className="text-muted p-6 text-center text-sm">No matches</p>
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
      </ScrollShadow>

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
