import { Card, Chip, Skeleton } from '@heroui/react'
import { useState } from 'react'
import { formatTimestamp } from '@/shared/lib/datetime.utils'
import { useSourceStatus } from '../hooks/useSources'
import type { SyncResult } from '../types/source.type'
import { SyncSourceButton } from './SyncSourceButton'

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex items-baseline justify-between gap-4">
      <span className="text-muted text-sm">{label}</span>
      <span className="text-foreground min-w-0 truncate text-sm font-medium">{children}</span>
    </div>
  )
}

/** Index freshness plus the sync trigger. Sync is idempotent server-side, so
 *  re-running it is safe; the result says whether anything actually changed. */
export function SourceIndexCard({ sourceId }: { sourceId: string }) {
  const { data, isPending } = useSourceStatus(sourceId)
  const [result, setResult] = useState<SyncResult | null>(null)

  return (
    <Card className="p-6">
      <Card.Header className="flex-row items-start justify-between">
        <Card.Title className="text-accent text-lg font-bold">Index</Card.Title>
        <SyncSourceButton sourceId={sourceId} onSynced={setResult} />
      </Card.Header>
      <Card.Content className="flex flex-col gap-3">
        {isPending && <Skeleton className="h-4 w-2/3 rounded" />}

        {data && !data.indexed && <p className="text-muted text-sm">Never synced</p>}

        {data?.indexed && (
          <>
            <Row label="Last update">{formatTimestamp(new Date(data.indexedAt!))}</Row>

            <Row label="Ref">
              <span>{data.ref ?? '—'}</span>
            </Row>

            <Row label="Commit">
              <span>{data.commitSha?.slice(0, 10) ?? '—'}</span>
            </Row>
          </>
        )}

        {result && (
          <div className="flex flex-wrap items-center gap-2 border-t pt-3">
            {result.idempotent ? (
              <Chip size="md" variant="soft">
                already up to date
              </Chip>
            ) : (
              <Chip size="md" variant="soft" color="success">
                updated
              </Chip>
            )}
            <span className="text-muted text-sm">
              + {result.indexed} indexed · {result.reindexed} re-indexed · {result.evicted} evicted
              · {result.skipped} skipped · {result.durationMs}ms
            </span>
          </div>
        )}
      </Card.Content>
    </Card>
  )
}
