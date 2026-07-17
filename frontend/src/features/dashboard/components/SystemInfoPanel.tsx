import { Card, Chip, Skeleton } from '@heroui/react'
import { formatTimestamp } from '@/shared/lib/datetime.utils'
import { useSystemInfo } from '../hooks/useSystemInfo'
import { deriveHealthStatus } from '../lib/health.util'

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex items-baseline justify-between gap-4">
      <span className="text-muted text-sm">{label}</span>
      <span className="text-foreground text-sm font-medium">{children}</span>
    </div>
  )
}

/** Runtime overview of the active backend: product, version, profiles, a derived
 *  health badge, and the time of the last successful read. Owns its own query so
 *  it can be dropped anywhere; TanStack Query dedupes the shared key. */
export function SystemInfoPanel() {
  const { data, isPending, isError, error, dataUpdatedAt } = useSystemInfo()
  const health = data ? deriveHealthStatus(data) : null

  return (
    <Card>
      <Card.Header className="flex-row items-center justify-between">
        <Card.Title>Runtime</Card.Title>
        {health && (
          <Chip color={health.tone} variant="soft" size="sm">
            {health.label}
          </Chip>
        )}
      </Card.Header>
      <Card.Content className="flex flex-col gap-3">
        {isPending && (
          <div className="flex flex-col gap-3">
            <Skeleton className="h-4 w-2/3 rounded" />
            <Skeleton className="h-4 w-1/2 rounded" />
            <Skeleton className="h-4 w-3/5 rounded" />
          </div>
        )}
        {isError && <p className="text-danger text-sm">{(error as Error).message}</p>}
        {data && (
          <>
            <Row label="Product">{data.application}</Row>
            <Row label="Version">{data.version}</Row>
            <Row label="Profiles">{data.activeProfiles.join(', ') || '—'}</Row>
            <Row label="Last update">{formatTimestamp(dataUpdatedAt)}</Row>
          </>
        )}
      </Card.Content>
    </Card>
  )
}
