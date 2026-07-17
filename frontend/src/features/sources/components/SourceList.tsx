import { Card, Skeleton } from '@heroui/react'
import type { Source } from '../types/source.type'
import { SourceCard } from './SourceCard'

interface SourceListProps {
  sources?: Source[]
  isPending: boolean
  onDelete?: (id: string) => void
}

/** Renders the source list with its own loading skeletons and empty state, so
 *  the page stays a thin orchestrator. */
export function SourceList({ sources, isPending, onDelete }: SourceListProps) {
  if (isPending) {
    return (
      <div className="flex flex-col gap-3">
        {Array.from({ length: 3 }).map((_, i) => (
          <Skeleton key={i} className="h-24 w-full rounded-2xl" />
        ))}
      </div>
    )
  }

  if (!sources || sources.length === 0) {
    return (
      <Card variant="transparent" className="border-dashed">
        <Card.Content className="text-muted py-10 text-center text-sm">
          No sources yet. Add one to get started.
        </Card.Content>
      </Card>
    )
  }

  return (
    <div className="flex flex-col gap-3">
      {sources.map((source) => (
        <SourceCard key={source.id} source={source} onDelete={onDelete} />
      ))}
    </div>
  )
}
