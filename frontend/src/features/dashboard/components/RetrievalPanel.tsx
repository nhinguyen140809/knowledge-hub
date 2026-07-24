import { Card, Skeleton } from '@heroui/react'
import { ErrorState } from '@/shared/components/ui/ErrorState'
import { useRetrievalStats } from '../hooks/useDashboardStats'

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex items-baseline justify-between gap-4">
      <span className="text-muted text-sm">{label}</span>
      <span className="text-foreground text-sm font-medium">{children}</span>
    </div>
  )
}

/** Retrieval activity — the product's actual job. Counts are since the backend
 *  last started, not all-time, which the "since start" note makes explicit. */
export function RetrievalPanel() {
  const { data, isPending, isError, error } = useRetrievalStats()

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
        <Row label="Queries served">{data.queriesServed.toLocaleString()}</Row>
        <Row label="Latency p50">{data.p50LatencyMs} ms</Row>
        <Row label="Latency p95">{data.p95LatencyMs} ms</Row>
        <Row label="Cache hit rate">{Math.round(data.cacheHitRate * 100)}%</Row>
      </>
    )
  }

  return (
    <Card className="px-6">
      <Card.Header className="flex-row items-center justify-between">
        <Card.Title className="text-accent text-lg font-bold">Retrieval</Card.Title>
        <span className="text-muted text-xs">since start</span>
      </Card.Header>
      <Card.Content className="flex flex-col gap-3">{content()}</Card.Content>
    </Card>
  )
}
