import { Card, Chip } from '@heroui/react'
import { Link } from 'react-router-dom'
import { formatTimestamp } from '@/shared/lib/datetime.utils'
import { SOURCE_TYPE_COLOR, SOURCE_TYPE_LABEL } from '../constants/source.config'
import type { Source } from '../types/source.type'
import { SyncSourceButton } from './SyncSourceButton'

/** One source row: name/id/description + location on the left, last-update
 *  status and a sync trigger on the right. The title opens the detail page. */
export function SourceCard({ source }: { source: Source }) {
  const location = source.ref ? `${source.uriOrPath} @ ${source.ref}` : source.uriOrPath
  const detailPath = `/sources/${encodeURIComponent(source.id)}`
  const lastUpdate = source.updatedAt
    ? `Updated ${formatTimestamp(new Date(source.updatedAt))}`
    : 'Never synced'

  return (
    <Card className="flex flex-row items-start justify-between gap-4 p-6">
      <Card.Header className="min-w-0 gap-1">
        <Card.Title>
          <div className="flex min-w-0 items-center gap-3">
            <Link
              to={detailPath}
              className="hover:text-focus text-lg font-bold underline-offset-4 hover:underline"
            >
              {source.name ?? source.id}
            </Link>
            <Chip size="sm" variant="soft" color={SOURCE_TYPE_COLOR[source.type]}>
              {SOURCE_TYPE_LABEL[source.type]}
            </Chip>
          </div>
        </Card.Title>

        <Card.Description className="text-sm">{source.id}</Card.Description>

        <p className="text-accent mt-1 truncate text-sm font-semibold">{location}</p>

        {source.description && <p className="text-muted mt-1 text-sm">{source.description}</p>}
      </Card.Header>
      <div className="flex shrink-0 flex-col items-end gap-2">
        <span className="text-muted text-xs">{lastUpdate}</span>
        <SyncSourceButton sourceId={source.id} label={source.name ?? source.id} isIconOnly />
      </div>
    </Card>
  )
}
