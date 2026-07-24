import { Button, Skeleton } from '@heroui/react'
import { ArrowLeft } from 'lucide-react'
import { useNavigate, useParams } from 'react-router-dom'
import { ErrorState } from '@/shared/components/ui/ErrorState'
import { renderLink } from '@/shared/lib/renderLink'
import { DeleteSourceButton } from '../components/DeleteSourceButton'
import { EditSourceDialog } from '../components/EditSourceDialog'
import { SourceGlobsCard } from '../components/SourceGlobsCard'
import { SourceIndexCard } from '../components/SourceIndexCard'
import { SourceSummaryCard } from '../components/SourceSummaryCard'
import { useSource } from '../hooks/useSources'

/** Everything about one source: what it is, what it ingests, and how fresh its
 *  index is. Summary spans the full width since it's the identity card; index
 *  and patterns sit side by side below it. */
export function SourceDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { data, isPending, isError, error } = useSource(id)

  function content() {
    if (isPending) return <Skeleton className="h-40 w-full rounded-2xl" />

    if (isError) return <ErrorState description={(error as Error).message} />
    
    if (!data) return null
    return (
      <div className="flex flex-col gap-4">
        <SourceSummaryCard source={data} />
        <div className="grid gap-4 lg:grid-cols-2">
          {id && <SourceIndexCard key={id} sourceId={id} />}
          <SourceGlobsCard source={data} />
        </div>
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between gap-4">
        <Button size="sm" variant="ghost" render={renderLink('/sources')}>
          <ArrowLeft size={16} />
          All sources
        </Button>
        {data && (
          <div className="flex items-center gap-2">
            <EditSourceDialog source={data} />
            <DeleteSourceButton
              sourceId={data.id}
              label={data.name ?? data.id}
              onDeleted={() => navigate('/sources')}
            />
          </div>
        )}
      </div>

      {content()}
    </div>
  )
}
