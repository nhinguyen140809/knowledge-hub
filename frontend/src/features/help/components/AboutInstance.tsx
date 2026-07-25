import { Card } from '@heroui/react'
import type { ReactNode } from 'react'
import { SystemInfoPanel } from '@/features/dashboard/components/SystemInfoPanel'
import { useActiveConnection } from '@/lib/store/connections.store'
import { NO_VALUE } from '@/shared/constants'

function InfoRow({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="flex items-baseline justify-between gap-4">
      <span className="text-muted text-sm">{label}</span>
      <span className="text-foreground min-w-0 truncate text-sm font-medium">{children}</span>
    </div>
  )
}

/** The About section: what this running instance is, reusing the dashboard's
 *  system panel and showing the active connection it is pointed at. */
export function AboutInstance() {
  const connection = useActiveConnection()

  return (
    <div id="about" className="flex scroll-mt-2 flex-col gap-4">
      <SystemInfoPanel />
      <Card className="px-6" variant="transparent">
        <Card.Header>
          <Card.Title className="text-accent text-lg font-bold">Connection</Card.Title>
        </Card.Header>
        <Card.Content className="flex flex-col gap-3">
          <InfoRow label="Name">{connection?.label ?? NO_VALUE}</InfoRow>
          <InfoRow label="URL">{connection?.baseUrl ?? NO_VALUE}</InfoRow>
        </Card.Content>
      </Card>
    </div>
  )
}
