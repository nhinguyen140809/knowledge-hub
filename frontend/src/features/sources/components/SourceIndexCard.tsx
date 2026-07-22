import { Card, Chip, Skeleton } from '@heroui/react'
import { useState } from 'react'
import { formatTimestamp } from '@/shared/lib/datetime.utils'
import { useSourceStatus } from '../hooks/useSources'
import type { SyncResult } from '../types/source.type'
import { SyncSourceButton } from './SyncSourceButton'
import { NO_VALUE } from '@/shared/constants'

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex items-baseline justify-between gap-4">
      <span className="text-muted text-sm">{label}</span>
      <span className="text-foreground min-w-0 truncate text-sm font-medium">{children}</span>
    </div>
  )
}

function Result({ result }: { result: SyncResult }) {
  return (
    <div className="text-sm font-bold flex gap-4">
      <span className="text-success">{result.indexed} indexed</span>
      <span className="text-warning">{result.reindexed} re-indexed</span>
      <span className="text-danger">{result.evicted} evicted</span>
      <span className="text-muted">{result.skipped} skipped</span>
      <span className="text-success">{result.durationMs}ms</span>
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
              <span>{data.ref ?? NO_VALUE}</span>
            </Row>

            <Row label="Commit">
              <span>{data.commitSha?.slice(0, 10) ?? NO_VALUE}</span>
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
            <Result result={result} />
          </div>
        )}
      </Card.Content>
    </Card>
  )
}
