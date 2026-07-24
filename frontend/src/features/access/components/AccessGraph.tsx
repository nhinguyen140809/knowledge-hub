import { Skeleton } from '@heroui/react'
import { GraphView } from '@/shared/components/ui/GraphView'
import { ErrorState } from '@/shared/components/ui/ErrorState'
import { useAccessGraphModel } from '../hooks/useAccessGraphModel'
import { isSourceNodeId } from '../lib/sourceNode'

interface AccessGraphProps {
  selectedId?: string | null
  onSelect?: (principalId: string) => void
  /** Source whose access path to light up — set when the user asks "why can it
   *  read this" from the details side, null otherwise. The path (nodes and
   *  edges) is derived by the model from the scoped graph. */
  traceSourceId?: string | null
}

/**
 * Hybrid view: with nothing selected, the whole membership graph as an
 * overview; with a principal selected, the scoped access-graph explaining
 * "*why* does this principal reach that source". The model (nodes, edges,
 * query state) comes from {@link useAccessGraphModel}; this component only
 * picks what each state renders as. Source nodes are display-only — clicking
 * one selects nothing.
 */
export function AccessGraph({ selectedId, onSelect, traceSourceId }: AccessGraphProps) {
  const { nodes, edges, highlightNodeIds, isPending, isError, error } = useAccessGraphModel(
    selectedId,
    traceSourceId,
  )

  if (isPending) return <Skeleton className="h-105 w-full rounded-xl lg:h-full" />

  if (isError) return <ErrorState description={(error as Error).message} />

  return (
    <GraphView
      nodes={nodes}
      edges={edges}
      direction="auto"
      layoutEngine="dagre"
      className="h-105 lg:h-full"
      highlightNodeIds={highlightNodeIds ?? undefined}
      onNodeClick={(id) => !isSourceNodeId(id) && onSelect?.(id)}
    />
  )
}
