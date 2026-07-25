import { Surface } from '@heroui/react'
import type { ReactNode } from 'react'

const NODE_W = 92
const NODE_H = 34

/** A labelled box in a concept diagram. Colour follows the current text colour,
 *  so the caller tints it by wrapping in a `text-*` class. */
function Node({
  x,
  y,
  label,
  accent = false,
}: {
  x: number
  y: number
  label: string
  accent?: boolean
}) {
  return (
    <g className={accent ? 'text-accent' : 'text-muted'}>
      <rect
        x={x}
        y={y}
        width={NODE_W}
        height={NODE_H}
        rx={8}
        className="fill-none stroke-current"
        strokeWidth={accent ? 2 : 1.5}
      />
      <text
        x={x + NODE_W / 2}
        y={y + NODE_H / 2}
        textAnchor="middle"
        dominantBaseline="central"
        className="fill-current text-[11px] font-medium"
      >
        {label}
      </text>
    </g>
  )
}

/** A connector between two points, optionally labelled and accented. */
function Edge({
  from,
  to,
  label,
  accent = false,
}: {
  from: [number, number]
  to: [number, number]
  label?: string
  accent?: boolean
}) {
  const [x1, y1] = from
  const [x2, y2] = to
  return (
    <g className={accent ? 'text-accent' : 'text-muted'}>
      <line
        x1={x1}
        y1={y1}
        x2={x2}
        y2={y2}
        className="stroke-current"
        strokeWidth={accent ? 2 : 1.5}
      />
      {label && (
        <text
          x={(x1 + x2) / 2 + 6}
          y={(y1 + y2) / 2}
          dominantBaseline="central"
          className="fill-current text-[10px]"
        >
          {label}
        </text>
      )}
    </g>
  )
}

/** Wraps a diagram with its caption on a HeroUI secondary surface. */
function Figure({
  viewBox,
  caption,
  children,
}: {
  viewBox: string
  caption: string
  children: ReactNode
}) {
  return (
    <Surface variant="secondary" className="mt-1 flex flex-col items-center gap-2 rounded-lg p-4">
      <svg
        viewBox={viewBox}
        className="h-auto w-full max-w-75"
        role="img"
        aria-label={caption}
      >
        {children}
      </svg>
      <p className="text-muted text-center text-xs">{caption}</p>
    </Surface>
  )
}

/** Membership is a graph, not a tree: one subject can belong to several groups. */
export function MembershipFigure() {
  return (
    <Figure viewBox="0 0 300 150" caption="One subject can belong to several groups at once.">
      <Node x={12} y={16} label="Group A" />
      <Node x={196} y={16} label="Group B" />
      <Node x={104} y={100} label="Subject" accent />
      <Edge from={[150, 100]} to={[58, 50]} />
      <Edge from={[150, 100]} to={[242, 50]} />
    </Figure>
  )
}

/** Access flows down membership: a subject inherits every source its groups hold. */
export function AccessInheritanceFigure() {
  return (
    <Figure
      viewBox="0 0 300 190"
      caption="A subject inherits every source granted to a group it belongs to."
    >
      <Node x={104} y={12} label="Source" />
      <Node x={104} y={78} label="Group" />
      <Node x={104} y={144} label="Subject" accent />
      <Edge from={[150, 78]} to={[150, 46]} label="grant" accent />
      <Edge from={[150, 144]} to={[150, 112]} label="member" />
    </Figure>
  )
}
