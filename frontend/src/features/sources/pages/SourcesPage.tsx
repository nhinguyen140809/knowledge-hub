import { Button } from '@heroui/react'
import { useQueryClient } from '@tanstack/react-query'
import { RefreshCw } from 'lucide-react'
import { sourceKeys } from '../api/sources.keys'
import { CreateSourceDialog } from '../components/CreateSourceDialog'
import { SourceList } from '../components/SourceList'
import { useSources } from '../hooks/useSources'

/** Sources management screen: toolbar (add / refresh) over the source list. */
export function SourcesPage() {
  const queryClient = useQueryClient()
  const { data, isPending, isFetching } = useSources()

  return (
    <div className="flex flex-col gap-6">
      <div className="flex justify-end gap-2">
        <CreateSourceDialog />
        <Button
          size="sm"
          variant="secondary"
          onPress={() => queryClient.invalidateQueries({ queryKey: sourceKeys.all })}
        >
          <RefreshCw size={16} className={isFetching ? 'animate-spin' : ''} />
          Refresh
        </Button>
      </div>

      <SourceList sources={data} isPending={isPending} />
    </div>
  )
}
