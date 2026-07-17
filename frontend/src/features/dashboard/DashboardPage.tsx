import { Card, Spinner } from '@heroui/react'
import { useQuery } from '@tanstack/react-query'
import { fetchSystemInfo } from '../../lib/api/system.api'
import { useActiveConnection } from '../../lib/store/connections.store'

/** Placeholder dashboard: proves the active-connection + TanStack Query wiring by
 *  reading the backend's system info. Real stats/tiles come later. */
export function DashboardPage() {
  const active = useActiveConnection()
  const { data, isPending, isError, error } = useQuery({
    queryKey: ['system-info', active?.id],
    queryFn: fetchSystemInfo,
    enabled: !!active,
  })

  return (
    <div className="grid gap-4 md:grid-cols-3">
      <Card>
        <Card.Header>
          <Card.Title>Backend</Card.Title>
        </Card.Header>
        <Card.Content className="flex flex-col gap-1">
          <p className="text-sm text-neutral-500">Active connection</p>
          <p className="font-medium">{active?.label}</p>
          <p className="break-all text-sm text-neutral-500">{active?.baseUrl}</p>
        </Card.Content>
      </Card>

      <Card>
        <Card.Header>
          <Card.Title>System info</Card.Title>
        </Card.Header>
        <Card.Content>
          {isPending && <Spinner />}
          {isError && <p className="text-sm text-red-600">{(error as Error).message}</p>}
          {data && (
            <ul className="flex flex-col gap-1 text-sm">
              <li>App: {data.application}</li>
              <li>Version: {data.version}</li>
              <li>Profiles: {data.activeProfiles.join(', ') || '—'}</li>
            </ul>
          )}
        </Card.Content>
      </Card>
    </div>
  )
}
