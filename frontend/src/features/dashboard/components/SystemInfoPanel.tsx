import { Card, Chip, Skeleton } from '@heroui/react'
import { ErrorState } from '@/shared/components/ui/ErrorState'
import { formatTimestamp } from '@/shared/lib/datetime.utils'
import { useSystemInfo } from '../hooks/useSystemInfo'
import { deriveHealthStatus } from '../lib/health.util'
import { NO_VALUE } from '@/shared/constants'

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

  function content() {
    if (isPending) {
      return (
        <div className="flex flex-col gap-3">
          <Skeleton className="h-4 w-2/3 rounded" />
          <Skeleton className="h-4 w-1/2 rounded" />
          <Skeleton className="h-4 w-3/5 rounded" />
        </div>
      )
    }
    if (isError) return <ErrorState description={(error as Error).message} />
    if (!data) return null
    return (
      <>
        <Row label="Product">{data.productName}</Row>
        <Row label="Version">{data.version}</Row>
        <Row label="Profiles">{data.activeProfiles.join(', ') || NO_VALUE}</Row>
        <Row label="Last update">{formatTimestamp(dataUpdatedAt)}</Row>
      </>
    )
  }

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
      <Card.Content className="flex flex-col gap-3">{content()}</Card.Content>
    </Card>
  )
}
