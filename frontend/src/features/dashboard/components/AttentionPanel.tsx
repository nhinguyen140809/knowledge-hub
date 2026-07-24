import { Card, Skeleton } from '@heroui/react'
import { CircleCheck, TriangleAlert } from 'lucide-react'
import { useSources } from '@/features/sources'
import { deriveAttentionItems, type AttentionItem } from '../lib/attention.util'
import { useDependencyHealth } from '../hooks/useDashboardStats'
import { useSystemInfo } from '../hooks/useSystemInfo'

function AttentionRow({ item }: { item: AttentionItem }) {
  return (
    <div className="flex items-start gap-2 text-sm">
      <TriangleAlert
        size={16}
        className={`mt-0.5 shrink-0 ${item.tone === 'danger' ? 'text-danger' : 'text-warning'}`}
      />
      <span className="text-foreground">{item.message}</span>
    </div>
  )
}

function AllClear() {
  return (
    <div className="text-muted flex items-center gap-2 text-sm">
      <CircleCheck size={16} className="text-success shrink-0" />
      All clear, nothing needs a look
    </div>
  )
}

/** The dashboard's "what needs a look" panel. Gathers the signals the rules
 *  read — all cached queries shared with the rest of the app — and renders the
 *  derived list, or an all-clear when nothing fires. The judgement of what
 *  counts lives entirely in {@link deriveAttentionItems}. */
export function AttentionPanel() {
  const dependencies = useDependencyHealth()
  const systemInfo = useSystemInfo()
  const sources = useSources()

  const isPending = dependencies.isPending || systemInfo.isPending || sources.isPending

  const items = deriveAttentionItems({
    dependencies: dependencies.data,
    systemInfo: systemInfo.data,
    sources: sources.data,
  })

  function content() {
    if (isPending) {
      return (
        <>
          <Skeleton className="h-5 w-3/4 rounded" />
          <Skeleton className="h-5 w-2/3 rounded" />
        </>
      )
    }
    if (items.length === 0) return <AllClear />

    return items.map((item) => <AttentionRow key={item.id} item={item} />)
  }

  return (
    <Card className="px-6">
      <Card.Header>
        <Card.Title className="text-accent text-lg font-bold">Attention</Card.Title>
      </Card.Header>
      <Card.Content className="flex flex-col gap-3">{content()}</Card.Content>
    </Card>
  )
}
