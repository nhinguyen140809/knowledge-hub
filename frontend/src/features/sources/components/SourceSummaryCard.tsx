import { Card, Chip } from '@heroui/react'
import type { Source } from '../types/source.type'

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex items-baseline justify-between gap-4">
      <span className="text-muted text-sm">{label}</span>
      <span className="text-foreground min-w-0 truncate text-sm font-medium">{children}</span>
    </div>
  )
}

/** Identity and location of a source: what it is and where it comes from. */
export function SourceSummaryCard({ source }: { source: Source }) {
  return (
    <Card>
      <Card.Header className="flex-row items-start justify-between">
        <div className="flex min-w-0 flex-col gap-1">
          <Card.Title>{source.name ?? source.id}</Card.Title>
          <Card.Description className="font-mono text-xs">{source.id}</Card.Description>
        </div>
        <Chip size="sm" variant="soft" color={source.type === 'GIT' ? 'accent' : 'default'}>
          {source.type}
        </Chip>
      </Card.Header>
      <Card.Content className="flex flex-col gap-3">
        {source.description && <p className="text-muted text-sm">{source.description}</p>}
        <Row label={source.type === 'GIT' ? 'Repository' : 'Path'}>
          <span className="font-mono">{source.uriOrPath}</span>
        </Row>
        <Row label="Ref">
          <span className="font-mono">{source.ref ?? '—'}</span>
        </Row>
      </Card.Content>
    </Card>
  )
}
