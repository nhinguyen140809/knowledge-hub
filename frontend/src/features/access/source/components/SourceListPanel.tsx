import { Card, ScrollShadow, Skeleton } from '@heroui/react'
import { Database } from 'lucide-react'
import { useSources } from '@/features/sources'
import { EmptyState } from '@/shared/components/ui/EmptyState'
import { ErrorState } from '@/shared/components/ui/ErrorState'
import { Tree } from '@/shared/components/ui/Tree'

interface SourceListPanelProps {
  selectedId: string | null
  onSelect: (id: string) => void
}

/**
 * The selector side of the Access-Control-Sources page: a flat list of
 * sources — unlike principals, sources don't nest into groups, so this has no
 * tree/membership logic. Source lifecycle (create/edit/delete) stays owned by
 * the standalone Sources page; this panel only selects.
 */
export function SourceListPanel({ selectedId, onSelect }: SourceListPanelProps) {
  const { data, isPending, isError, error } = useSources()

  function content() {
    if (isPending) {
      return (
        <div className="flex flex-col gap-2">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-8 w-full rounded-lg" />
          ))}
        </div>
      )
    }
    if (isError) return <ErrorState description={(error as Error).message} />
    if (!data || data.length === 0) {
      return <EmptyState icon={<Database size={28} />} description="No sources configured yet." />
    }
    return (
      <Tree>
        {data.map((source) => (
          <Tree.Item
            key={source.id}
            label={source.name ?? source.id}
            icon={<Database size={14} className="text-muted" />}
            isSelected={selectedId === source.id}
            onSelect={() => onSelect(source.id)}
          />
        ))}
      </Tree>
    )
  }

  return (
    <Card
      variant="transparent"
      className="self-start lg:flex lg:min-h-0 lg:flex-col lg:self-stretch"
    >
      <Card.Header>
        <Card.Title className="text-accent text-lg font-bold">Sources</Card.Title>
      </Card.Header>
      <Card.Content className="lg:flex lg:min-h-0 lg:flex-1 lg:flex-col">
        <ScrollShadow className="lg:min-h-0 lg:flex-1" offset={2}>
          {content()}
        </ScrollShadow>
        {data && <p className="text-muted text-xs">{data.length} sources</p>}
      </Card.Content>
    </Card>
  )
}
