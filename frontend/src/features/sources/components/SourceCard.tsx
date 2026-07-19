import { Card, Chip } from '@heroui/react'
import { Link } from 'react-router-dom'
import type { Source } from '../types/source.type'
import { DeleteSourceButton } from './DeleteSourceButton'

/** One source row: name/id/description + location on the left, a type chip and
 *  delete on the right. The title opens the source's detail page. */
export function SourceCard({ source }: { source: Source }) {
  return (
    <Card>
      <div className="flex items-start justify-between gap-4">
        <Card.Header className="min-w-0 gap-1">
          <Card.Title>
            <Link
              to={`/sources/${encodeURIComponent(source.id)}`}
              className="hover:text-accent underline-offset-4 hover:underline"
            >
              {source.name ?? source.id}
            </Link>
          </Card.Title>
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
          <DeleteSourceButton sourceId={source.id} label={source.name ?? source.id} />
        </div>
      </div>
    </Card>
  )
}
