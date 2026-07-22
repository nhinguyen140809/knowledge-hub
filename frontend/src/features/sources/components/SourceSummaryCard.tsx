import { Card, Chip } from '@heroui/react'
import {
  SOURCE_TYPE_COLOR,
  SOURCE_TYPE_LABEL,
  SOURCE_TYPE_LOCATION,
} from '../constants/source.config'
import type { Source } from '../types/source.type'
import { NO_VALUE } from '@/shared/constants'

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex items-baseline gap-2">
      <span className="text-muted text-sm">{label}:</span>
      <span className="text-foreground min-w-0 truncate text-sm font-medium">{children}</span>
    </div>
  )
}

/** Identity and location of a source: what it is and where it comes from. */
export function SourceSummaryCard({ source }: { source: Source }) {
  const { hasRef, summaryLabel } = SOURCE_TYPE_LOCATION[source.type]

  return (
    <Card variant="transparent">
      <Card.Header className="flex-col items-start">
        <div className="mb-2 flex min-w-0 flex-row items-center gap-4">
          <Card.Title className="text-accent text-2xl font-bold">
            {source.name ?? source.id}
          </Card.Title>
          <Chip size="md" variant="soft" color={SOURCE_TYPE_COLOR[source.type]}>
            {SOURCE_TYPE_LABEL[source.type]}
          </Chip>
        </div>
        <Card.Description className="text-sm">{source.id}</Card.Description>
      </Card.Header>
      <Card.Content className="flex flex-col gap-3">
        {source.description && <p className="text-muted text-sm">{source.description}</p>}

        <Row label={summaryLabel}>
          <span className="text-accent">{source.uriOrPath}</span>
        </Row>

        {hasRef && (
          <Row label="Ref">
            <span>{source.ref ?? NO_VALUE}</span>
          </Row>
        )}
      </Card.Content>
    </Card>
  )
}
