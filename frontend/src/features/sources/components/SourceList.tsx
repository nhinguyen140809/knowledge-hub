import { Skeleton } from '@heroui/react'
import { Database, FilterX } from 'lucide-react'
import { EmptyState } from '@/shared/components/ui/EmptyState'
import type { Source } from '../types/source.type'
import { SourceCard } from './SourceCard'

interface SourceListProps {
  sources?: Source[]
  isPending: boolean
  /** True when the type filter is hiding otherwise-existing sources, so the
   *  empty state can explain that instead of suggesting to add one. */
  isFiltered?: boolean
}

/** Renders the source list with its own loading skeletons and empty state, so
 *  the page stays a thin orchestrator. */
export function SourceList({ sources, isPending, isFiltered }: SourceListProps) {
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
    return isFiltered ? (
      <EmptyState icon={<FilterX size={28} />} description="No sources match this filter." />
    ) : (
      <EmptyState
        icon={<Database size={28} />}
        description="No sources yet. Add one to get started."
      />
    )
  }

  return (
    <div className="flex flex-col gap-3">
      {sources.map((source) => (
        <SourceCard key={source.id} source={source} />
      ))}
    </div>
  )
}
