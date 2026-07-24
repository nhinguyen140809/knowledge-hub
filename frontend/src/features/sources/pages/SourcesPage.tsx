import { Button, ScrollShadow } from '@heroui/react'
import { RotateCw } from 'lucide-react'
import { useMemo, useState } from 'react'
import { SUMMARY_SEP } from '@/shared/constants'
import { CreateSourceDialog } from '../components/CreateSourceDialog'
import { SourceFilterSort } from '../components/SourceFilterSort'
import { SourceList } from '../components/SourceList'
import { useInvalidateSources } from '../hooks/useSourceMutations'
import { useSources } from '../hooks/useSources'
import { applySourceFilterSort, DEFAULT_SOURCE_FILTER_SORT } from '../lib/sourceFilterSort'

/** Sources management screen: toolbar (filter/sort, add, refresh) over the
 *  source list. */
export function SourcesPage() {
  const refresh = useInvalidateSources()
  const { data, isPending, isFetching } = useSources()
  const [filterSort, setFilterSort] = useState(DEFAULT_SOURCE_FILTER_SORT)

  const sources = useMemo(
    () => (data ? applySourceFilterSort(data, filterSort) : undefined),
    [data, filterSort],
  )

  // Describes the whole collection, not the filtered view — a caption for what
  // exists, independent of the current filter. Git/FS is the one split not
  // obvious from a glance at the list.
  const summary = data
    ? [
        `${data.length} sources`,
        `${data.filter((s) => s.type === 'GIT').length} Git`,
        `${data.filter((s) => s.type === 'FS').length} filesystem`,
      ].join(SUMMARY_SEP)
    : null

  return (
    <div className="flex h-full flex-col gap-6">
      <div className="flex shrink-0 items-center justify-between gap-2">
        <SourceFilterSort value={filterSort} onChange={setFilterSort} />
        <div className="flex gap-2">
          <CreateSourceDialog />
          <Button size="sm" variant="primary" onPress={refresh}>
            <RotateCw size={16} className={isFetching ? 'animate-spin' : ''} />
            Refresh
          </Button>
        </div>
      </div>

      {summary && <p className="text-muted -mt-4 shrink-0 text-sm">{summary}</p>}

      {/* offset absorbs the subpixel gap between scrollTop and scrollHeight on
          scaled displays, otherwise the bottom shadow never clears at the end. */}
      <ScrollShadow className="min-h-0 flex-1" offset={2}>
        <SourceList
          sources={sources}
          isPending={isPending}
          isFiltered={filterSort.type !== 'ALL' && !!data && data.length > 0}
        />
      </ScrollShadow>
    </div>
  )
}
