import { Card, Chip, Skeleton } from '@heroui/react'
import { formatTimestamp } from '@/shared/lib/datetime.utils'
import type { Hit } from '../types/query.type'

function lineRange(hit: Hit): string | null {
  const { lineStart, lineEnd } = hit.metadata
  if (lineStart == null) return null
  return lineEnd != null && lineEnd !== lineStart ? `${lineStart}–${lineEnd}` : `${lineStart}`
}

function HitCard({ hit }: { hit: Hit }) {
  const lines = lineRange(hit)
  return (
    <Card>
      <div className="flex items-start justify-between gap-4">
        <Card.Header className="min-w-0 gap-1">
          <Card.Title className="truncate font-mono text-sm">
            {hit.metadata.path}
            {lines && <span className="text-muted">:{lines}</span>}
          </Card.Title>
          <Card.Description className="text-xs">
            {hit.metadata.sourceId}
            {hit.metadata.ref && ` @ ${hit.metadata.ref}`}
            {hit.metadata.commitSha && ` · ${hit.metadata.commitSha.slice(0, 8)}`}
            {hit.metadata.indexedAt &&
              ` · indexed ${formatTimestamp(new Date(hit.metadata.indexedAt))}`}
          </Card.Description>
          {hit.metadata.viaPath.length > 0 && (
            <p className="text-muted mt-1 text-xs">
              reached via {hit.metadata.viaPath.join(' → ')}
            </p>
          )}
        </Card.Header>
        <div className="flex shrink-0 flex-col items-end gap-2">
          <Chip size="sm" variant="soft" color="accent">
            {hit.relevanceScore.toFixed(3)}
          </Chip>
          <Chip size="sm" variant="secondary">
            {hit.metadata.type ?? hit.metadata.kind}
          </Chip>
        </div>
      </div>
    </Card>
  )
}

interface HitListProps {
  hits?: Hit[]
  isPending: boolean
  hasSearched: boolean
}

/** Ranked results, best first. */
export function HitList({ hits, isPending, hasSearched }: HitListProps) {
  if (!hasSearched) {
    return (
      <Card variant="transparent" className="border border-dashed">
        <Card.Content className="text-muted py-10 text-center text-sm">
          Run a query to see results.
        </Card.Content>
      </Card>
    )
  }

  if (isPending) {
    return (
      <div className="flex flex-col gap-3">
        {Array.from({ length: 3 }).map((_, i) => (
          <Skeleton key={i} className="h-24 w-full rounded-2xl" />
        ))}
      </div>
    )
  }

  if (!hits || hits.length === 0) {
    return (
      <Card variant="transparent" className="border border-dashed">
        <Card.Content className="text-muted py-10 text-center text-sm">
          No results. Nothing indexed matches, or the sources you can read do not contain it.
        </Card.Content>
      </Card>
    )
  }

  return (
    <div className="flex flex-col gap-3">
      {hits.map((hit) => (
        <HitCard key={hit.id} hit={hit} />
      ))}
    </div>
  )
}
