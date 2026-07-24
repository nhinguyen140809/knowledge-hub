/* eslint-disable react-refresh/only-export-components --
 * The compound `Tree.Item` / `Tree.Group` API (the shape HeroUI's own components
 * use) makes the sole export an Object.assign result, which the rule cannot
 * recognise as a component. The cost is that editing this file triggers a full
 * reload instead of a hot update; nothing breaks at runtime. */
import { Button, Dropdown } from '@heroui/react'
import { ChevronRight } from 'lucide-react'
import { createContext, useContext, useState, type CSSProperties, type ReactNode } from 'react'

/**
 * App-agnostic tree primitives (anatomy modelled on HeroUI's FileTree): a root
 * that sets the indent step, expandable groups, and leaf items. It renders
 * whatever nodes it is handed and holds no data of its own — deciding what the
 * nodes are, and guarding against repeats or cycles in a graph, belongs to the
 * caller that owns the data.
 *
 * Nesting is expressed by nested lists rather than per-row padding. The
 * optional guide line's inline-start offset is pinned to the toggle button's
 * center (not the indent step), so it always lines up under the chevron of
 * the row it belongs to.
 */

/** Half the toggle button's size (size-6 = 24px), so a nested group's guide
 *  line sits under its parent chevron's center rather than the indent step. */
const CHEVRON_CENTER = 12

interface TreeContextValue {
  level: number
  showGuideLines: boolean
}

const TreeContext = createContext<TreeContextValue>({ level: 1, showGuideLines: true })

interface TreeRowProps {
  label: ReactNode
  icon?: ReactNode
  /** Rendered at the end of the row — a chip, a count, an action. */
  trailing?: ReactNode
  isSelected?: boolean
  onSelect?: () => void
  /** Right-click menu content for this row — typically a `Dropdown.Menu`.
   *  Omit for no menu. The tree has no opinion on what actions make sense;
   *  it only wires up the right-click gesture and the menu's anchor. */
  contextMenu?: ReactNode
}

/** The chevron and the label are separate buttons: nesting a toggle inside a
 *  selectable row would be invalid markup and traps keyboard users. */
function TreeRow({
  label,
  icon,
  trailing,
  isSelected,
  onSelect,
  contextMenu,
  isExpandable = false,
  isExpanded = false,
  onToggle,
}: TreeRowProps & {
  isExpandable?: boolean
  isExpanded?: boolean
  onToggle?: () => void
}) {
  // The anchor is a zero-size point at the cursor, not the row's bounding box,
  // so the menu opens where the click actually happened instead of always in
  // the same spot relative to the row.
  const [menuAnchor, setMenuAnchor] = useState<{ x: number; y: number } | null>(null)

  return (
    <div
      className="relative flex items-center gap-1"
      onContextMenu={
        contextMenu
          ? (e) => {
              e.preventDefault()
              const rect = e.currentTarget.getBoundingClientRect()
              setMenuAnchor({ x: e.clientX - rect.left, y: e.clientY - rect.top })
            }
          : undefined
      }
    >
      {isExpandable ? (
        <Button
          isIconOnly
          size="sm"
          variant="ghost"
          className="size-6 shrink-0"
          aria-label={isExpanded ? 'Collapse' : 'Expand'}
          onPress={onToggle}
        >
          <ChevronRight
            size={14}
            className={`transition-transform ${isExpanded ? 'rotate-90' : ''}`}
          />
        </Button>
      ) : (
        <span aria-hidden className="size-6 shrink-0" />
      )}
      <Button
        fullWidth
        size="sm"
        variant={isSelected ? 'tertiary' : 'ghost'}
        className="min-w-0 justify-start font-normal"
        onPress={onSelect}
      >
        {icon}
        <span className="truncate">{label}</span>
        {trailing && <span className="ml-auto shrink-0">{trailing}</span>}
      </Button>
      {contextMenu && (
        <Dropdown
          isOpen={menuAnchor !== null}
          onOpenChange={(isOpen) => !isOpen && setMenuAnchor(null)}
        >
          {/* Invisible, zero-size anchor pinned to the click position — the
           *  point itself, not the trigger's edges, is where the menu opens. */}
          <Dropdown.Trigger
            aria-hidden
            excludeFromTabOrder
            className="pointer-events-none absolute size-0"
            style={menuAnchor ? { left: menuAnchor.x, top: menuAnchor.y } : undefined}
          />
          <Dropdown.Popover placement="bottom left">{contextMenu}</Dropdown.Popover>
        </Dropdown>
      )}
    </div>
  )
}

interface TreeProps {
  children: ReactNode
  /** Indent step per level, in pixels. */
  indent?: number
  showGuideLines?: boolean
  className?: string
}

function TreeRoot({ children, indent = 18, showGuideLines = true, className = '' }: TreeProps) {
  return (
    <TreeContext value={{ level: 1, showGuideLines }}>
      <ul
        role="tree"
        className={`flex flex-col gap-0.5 ${className}`}
        style={{ '--tree-indent': `${indent}px` } as CSSProperties}
      >
        {children}
      </ul>
    </TreeContext>
  )
}

/** A leaf row. */
function TreeItem(props: TreeRowProps) {
  const { level } = useContext(TreeContext)
  return (
    <li role="treeitem" aria-level={level} aria-selected={props.isSelected ?? undefined}>
      <TreeRow {...props} />
    </li>
  )
}

interface TreeGroupProps extends TreeRowProps {
  children: ReactNode
  defaultExpanded?: boolean
  /** Controlled expansion — pair with onExpandedChange to lazy-load children. */
  isExpanded?: boolean
  onExpandedChange?: (isExpanded: boolean) => void
}

/** An expandable row. The chevron only appears here, so leaves never show one. */
function TreeGroup({
  children,
  defaultExpanded = false,
  isExpanded: controlledExpanded,
  onExpandedChange,
  ...row
}: TreeGroupProps) {
  const { level, showGuideLines } = useContext(TreeContext)
  const [uncontrolledExpanded, setUncontrolledExpanded] = useState<boolean>(defaultExpanded)
  const isExpanded = controlledExpanded ?? uncontrolledExpanded

  const toggle = () => {
    const next = !isExpanded
    setUncontrolledExpanded(next)
    onExpandedChange?.(next)
  }

  return (
    <li role="treeitem" aria-level={level} aria-expanded={isExpanded}>
      <TreeRow {...row} isExpandable isExpanded={isExpanded} onToggle={toggle} />
      {isExpanded && (
        <TreeContext value={{ level: level + 1, showGuideLines }}>
          <ul
            role="group"
            className={`mt-0.5 flex flex-col gap-0.5 ${showGuideLines ? 'border-l' : ''}`}
            style={
              showGuideLines
                ? {
                    marginInlineStart: CHEVRON_CENTER,
                    paddingInlineStart: `calc(var(--tree-indent) - ${CHEVRON_CENTER}px)`,
                  }
                : { marginInlineStart: 'var(--tree-indent)' }
            }
          >
            {children}
          </ul>
        </TreeContext>
      )}
    </li>
  )
}

export const Tree = Object.assign(TreeRoot, { Item: TreeItem, Group: TreeGroup })
