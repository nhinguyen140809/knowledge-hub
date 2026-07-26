import { Skeleton } from '@heroui/react'
import { MousePointerClick } from 'lucide-react'
import { useState } from 'react'
import { EmptyState } from '@/shared/components/ui/EmptyState'
import { ErrorState } from '@/shared/components/ui/ErrorState'
import { GraphView } from '@/shared/components/ui/GraphView'
import { useSourceAccessGraphModel } from '../hooks/useSourceAccessGraphModel'
import { isSourceNodeId } from '../lib/sourceNode'

/**
 * The scoped subgraph explaining who can read one source: the source, every
 * principal a grant reaches, and the membership/grant edges between them.
 * Clicking a principal rings it (a local highlight, not a page navigation —
 * there's only one source in this view, so there's nowhere else to focus);
 * the source node stays display-only, same as `AccessGraph` excludes it.
 * Clicking an edge highlights it and its two ends via `GraphView` itself.
 */
export function SourceAccessGraph({ sourceId }: { sourceId: string | null }) {
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null)
  const { nodes, edges, isPending, isError, error } = useSourceAccessGraphModel(
    sourceId ?? undefined,
    selectedNodeId,
  )

  if (!sourceId) {
    return (
      <EmptyState
        icon={<MousePointerClick size={28} />}
        description="Select a source to see its access graph"
      />
    )
  }

  if (isPending) return <Skeleton className="h-105 w-full rounded-xl lg:h-full" />

  if (isError) return <ErrorState description={(error as Error).message} />

  return (
    <GraphView
      nodes={nodes}
      edges={edges}
      direction="auto"
      layoutEngine="dagre"
      className="h-105 lg:h-full"
      onNodeClick={(id) =>
        !isSourceNodeId(id) && setSelectedNodeId((prev) => (prev === id ? null : id))
      }
    />
  )
}
