import { Card, Chip, Skeleton } from '@heroui/react'
import { ChevronRight, Search, SearchX } from 'lucide-react'
import { Fragment } from 'react'
import { EmptyState } from '@/shared/components/ui/EmptyState'
import { SUMMARY_SEP } from '@/shared/constants'
import { formatTimestamp } from '@/shared/lib/datetime.utils'
import type { Hit } from '../types/query.type'

function lineRange(hit: Hit): string | null {
  const { lineStart, lineEnd } = hit.metadata
  if (lineStart == null) return null
  return lineEnd != null && lineEnd !== lineStart ? `${lineStart}-${lineEnd}` : `${lineStart}`
}

function hitDescription(hit: Hit): string {
  const { sourceId, ref, commitSha, indexedAt } = hit.metadata
  // sourceId and its ref read as one unit ("repo @ main"); the commit and
  // index time are separate facets
  const parts = [ref ? `${sourceId} @ ${ref}` : sourceId]
  if (commitSha) parts.push(commitSha.slice(0, 8))
  if (indexedAt) parts.push(`indexed ${formatTimestamp(new Date(indexedAt))}`)
  return parts.join(SUMMARY_SEP)
}

/** The graph traversal that reached this hit, as a breadcrumb trail — null
 *  for a direct match with nothing to walk through. */
function viaPathTrail(hit: Hit): React.ReactNode {
  const { viaPath } = hit.metadata
  if (viaPath.length === 0) return null

  return viaPath.map((step, i) => (
    <Fragment key={i}>
      {i > 0 && <ChevronRight size={12} className="inline shrink-0" />}
      {step}
    </Fragment>
  ))
}

function HitCard({ hit }: { hit: Hit }) {
  const lines = lineRange(hit)
  const description = hitDescription(hit)
  const trail = viaPathTrail(hit)
  return (
    <Card className="px-6">
      <div className="flex items-start justify-between gap-4">
        <Card.Header className="min-w-0 gap-1">
          <Card.Title className="text-accent truncate text-sm font-bold">
            {hit.metadata.path}
            {lines && <span className="text-foreground">:{lines}</span>}
          </Card.Title>
          <Card.Description className="text-xs">{description}</Card.Description>
          {trail && (
            <p className="text-muted mt-1 flex items-center gap-1 text-xs">reached via {trail}</p>
          )}
        </Card.Header>
        <div className="flex shrink-0 flex-col items-end gap-2">
          <Chip size="md" variant="soft" color="accent">
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
    return <EmptyState icon={<Search size={28} />} description="Run a query to see results." />
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
      <EmptyState
        icon={<SearchX size={28} />}
        description="No results. Nothing indexed matches, or the sources you can read do not contain it."
      />
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
