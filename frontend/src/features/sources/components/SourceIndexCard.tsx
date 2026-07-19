import { Button, Card, Chip, Skeleton } from '@heroui/react'
import { RefreshCw } from 'lucide-react'
import { formatTimestamp } from '@/shared/lib/datetime.utils'
import { useSyncSource } from '../hooks/useSourceMutations'
import { useSourceStatus } from '../hooks/useSources'

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
  const sync = useSyncSource()

  return (
    <Card>
      <Card.Header className="flex-row items-center justify-between">
        <Card.Title>Index</Card.Title>
        <Button
          size="sm"
          variant="secondary"
          isPending={sync.isPending}
          onPress={() => sync.mutate(sourceId)}
        >
          <RefreshCw size={16} className={sync.isPending ? 'animate-spin' : ''} />
          Sync
        </Button>
      </Card.Header>
      <Card.Content className="flex flex-col gap-3">
        {isPending && <Skeleton className="h-4 w-2/3 rounded" />}

        {data && !data.indexed && <p className="text-muted text-sm">Never synced.</p>}

        {data?.indexed && (
          <>
            <Row label="Last update">{formatTimestamp(new Date(data.indexedAt!))}</Row>
            <Row label="Ref">
              <span className="font-mono">{data.ref ?? '—'}</span>
            </Row>
            <Row label="Commit">
              <span className="font-mono">{data.commitSha?.slice(0, 10) ?? '—'}</span>
            </Row>
          </>
        )}

        {sync.data && (
          <div className="flex flex-wrap items-center gap-2 border-t pt-3">
            {sync.data.idempotent ? (
              <Chip size="sm" variant="soft">
                already up to date
              </Chip>
            ) : (
              <Chip size="sm" variant="soft" color="success">
                updated
              </Chip>
            )}
            <span className="text-muted text-xs">
              +{sync.data.indexed} indexed · {sync.data.reindexed} re-indexed · {sync.data.evicted}{' '}
              evicted · {sync.data.skipped} skipped · {sync.data.durationMs}ms
            </span>
          </div>
        )}
      </Card.Content>
    </Card>
  )
}
