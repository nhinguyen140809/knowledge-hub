import { Button, Skeleton } from '@heroui/react'
import { ArrowLeft } from 'lucide-react'
import { useNavigate, useParams } from 'react-router-dom'
import { renderLink } from '@/shared/lib/renderLink'
import { DeleteSourceButton } from '../components/DeleteSourceButton'
import { SourceGlobsCard } from '../components/SourceGlobsCard'
import { SourceIndexCard } from '../components/SourceIndexCard'
import { SourceSummaryCard } from '../components/SourceSummaryCard'
import { useSource } from '../hooks/useSources'

/** Everything about one source: what it is, what it ingests, and how fresh its
 *  index is. */
export function SourceDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { data, isPending, isError, error } = useSource(id)

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between gap-4">
        <Button size="sm" variant="ghost" render={renderLink('/sources')}>
          <ArrowLeft size={16} />
          All sources
        </Button>
        {id && data && (
          <DeleteSourceButton
            sourceId={id}
            label={data.name ?? data.id}
            onDeleted={() => navigate('/sources')}
          />
        )}
      </div>

      {isPending && <Skeleton className="h-40 w-full rounded-2xl" />}
      {isError && <p className="text-danger text-sm">{(error as Error).message}</p>}

      {data && (
        <div className="grid gap-4 lg:grid-cols-2">
          <SourceSummaryCard source={data} />
          {id && <SourceIndexCard sourceId={id} />}
          <SourceGlobsCard source={data} />
        </div>
      )}
    </div>
  )
}
