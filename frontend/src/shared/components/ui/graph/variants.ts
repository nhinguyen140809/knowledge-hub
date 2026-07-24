/** A node's visual identity — named for what it looks like, not what it's
 *  for. Which variant a given node *kind* gets (a group, a source, ...) is
 *  the caller's business; this only owns what each variant renders as. */
export type GraphNodeVariant = 'accent' | 'neutral' | 'success' | 'warning' | 'danger'

interface GraphNodeVariantStyle {
  background: string
  color: string
  borderColor: string
  borderStyle: 'solid' | 'dashed'
}

export const GRAPH_NODE_VARIANTS: Record<GraphNodeVariant, GraphNodeVariantStyle> = {
  accent: {
    background: 'var(--accent-soft)',
    color: 'var(--accent-soft-foreground)',
    borderColor: 'var(--accent)',
    borderStyle: 'solid',
  },
  neutral: {
    background: 'var(--default-soft)',
    color: 'var(--default-soft-foreground)',
    borderColor: 'var(--default)',
    borderStyle: 'dashed',
  },
  success: {
    background: 'var(--success-soft)',
    color: 'var(--success-soft-foreground)',
    borderColor: 'var(--success)',
    borderStyle: 'solid',
  },
  warning: {
    background: 'var(--warning-soft)',
    color: 'var(--warning-soft-foreground)',
    borderColor: 'var(--warning)',
    borderStyle: 'solid',
  },
  danger: {
    background: 'var(--danger-soft)',
    color: 'var(--danger-soft-foreground)',
    borderColor: 'var(--danger)',
    borderStyle: 'solid',
  },
}

export interface GraphNodeData extends Record<string, unknown> {
  label: string
  /** @default 'neutral' */
  variant?: GraphNodeVariant
  isSelected?: boolean
}
