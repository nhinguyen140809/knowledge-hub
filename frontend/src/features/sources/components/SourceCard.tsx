import { Button, Card, Chip } from '@heroui/react'
import { Trash2 } from 'lucide-react'
import type { Source } from '../types/source.type'

interface SourceCardProps {
  source: Source
  onDelete?: (id: string) => void
}

/** One source row: name/id/description + location on the left, a type chip and
 *  delete on the right. Presentational — the page owns what delete does. */
export function SourceCard({ source, onDelete }: SourceCardProps) {
  return (
    <Card>
      <div className="flex items-start justify-between gap-4">
        <Card.Header className="min-w-0 gap-1">
          <Card.Title>{source.name ?? source.id}</Card.Title>
          <Card.Description className="font-mono text-xs">{source.id}</Card.Description>
          {source.description && <p className="text-muted mt-1 text-sm">{source.description}</p>}
          <p className="text-muted mt-1 truncate font-mono text-xs">
            {source.uriOrPath}
            {source.ref ? ` @ ${source.ref}` : ''}
          </p>
        </Card.Header>
        <div className="flex shrink-0 flex-col items-end gap-2">
          <Chip size="sm" variant="soft" color={source.type === 'GIT' ? 'accent' : 'default'}>
            {source.type}
          </Chip>
          <Button
            isIconOnly
            size="sm"
            variant="danger"
            aria-label={`Delete ${source.name ?? source.id}`}
            onPress={() => onDelete?.(source.id)}
          >
            <Trash2 size={16} />
          </Button>
        </div>
      </div>
    </Card>
  )
}
