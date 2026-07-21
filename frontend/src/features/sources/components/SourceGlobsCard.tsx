import { Card, Chip } from '@heroui/react'
import type { Source } from '../types/source.type'

function GlobList({
  label,
  patterns,
  empty,
}: {
  label: string
  patterns: string[]
  empty: string
}) {
  return (
    <div className="flex flex-col gap-2">
      <span className="text-muted text-sm">{label}</span>
      {patterns.length === 0 ? (
        <span className="text-muted text-sm italic">{empty}</span>
      ) : (
        <div className="flex flex-wrap gap-2">
          {patterns.map((pattern) => (
            <Chip key={pattern} size="md" variant="secondary">
              <span>{pattern}</span>
            </Chip>
          ))}
        </div>
      )}
    </div>
  )
}

/** Which files the source contributes. Changing these only takes effect on the
 *  next sync, which is why they sit next to the index panel. */
export function SourceGlobsCard({ source }: { source: Source }) {
  return (
    <Card className="p-6">
      <Card.Header>
        <Card.Title className="text-accent text-lg font-bold">Patterns</Card.Title>
      </Card.Header>
      <Card.Content className="flex flex-col gap-4">
        <GlobList label="Include" patterns={source.include} empty="everything" />
        <GlobList label="Ignore" patterns={source.ignore} empty="nothing" />
      </Card.Content>
    </Card>
  )
}
