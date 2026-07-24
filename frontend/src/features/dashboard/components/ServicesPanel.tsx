import { Card, Chip, Skeleton } from '@heroui/react'
import { ErrorState } from '@/shared/components/ui/ErrorState'
import { useDependencyHealth } from '../hooks/useDashboardStats'
import type { DependencyState } from '../types/dashboard.type'

/** Pill color per dependency state — UP is calm, DOWN is loud, UNKNOWN sits
 *  between (reachable check inconclusive, not confirmed down). */
const STATE_COLOR: Record<DependencyState, 'success' | 'danger' | 'warning'> = {
  UP: 'success',
  DOWN: 'danger',
  UNKNOWN: 'warning',
}

/** Reachability of the backend's dependencies, one pill each. A single glance
 *  at whether Neo4j, Qdrant and the embedding provider are answering. */
export function ServicesPanel() {
  const { data, isPending, isError, error } = useDependencyHealth()

  function content() {
    if (isPending) return <Skeleton className="h-6 w-40 rounded-full" />

    if (isError) return <ErrorState description={(error as Error).message} />

    return data.map((dep) => (
      <Chip key={dep.name} size="sm" variant="soft" color={STATE_COLOR[dep.status]}>
        {dep.name} {dep.status}
      </Chip>
    ))
  }

  return (
    <Card className="px-6">
      <Card.Header>
        <Card.Title className="text-accent text-lg font-bold">Services</Card.Title>
      </Card.Header>
      <Card.Content className="flex flex-wrap gap-2">{content()}</Card.Content>
    </Card>
  )
}
