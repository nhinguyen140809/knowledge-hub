import { Card, ScrollShadow } from '@heroui/react'
import { SUMMARY_SEP } from '@/shared/constants'
import { usePrincipalGraph } from '../hooks/usePrincipals'
import { summarizePrincipals } from '../lib/principal.rules'
import { AddPrincipalDialog } from './AddPrincipalDialog'
import { PrincipalTree } from './PrincipalTree'

interface PrincipalsPanelProps {
  selectedId: string | null
  onSelect: (id: string) => void
  onDeleted: (id: string) => void
}

/**
 * The selector side of the Access page: the principal tree with its add control
 * and a live count breakdown. Self-contained — it reads the (cached) principal
 * graph itself both to render the tree and to summarise it, so the page only
 * has to pass selection wiring.
 */
export function PrincipalsPanel({ selectedId, onSelect, onDeleted }: PrincipalsPanelProps) {
  // Cached — the tree renders from this same query, so reading it here is free.
  // A live breakdown replaces the static caption once data lands; the counts
  // (groups, admins) aren't obvious from the tree alone.
  const graph = usePrincipalGraph()
  const summary = graph.data ? summarizePrincipals(graph.data.principals) : null
  const caption = summary
    ? [`${summary.total} principals`, `${summary.groups} groups`, `${summary.admins} admins`].join(
        SUMMARY_SEP,
      )
    : 'Groups and subjects'

  return (
    <Card
      variant="transparent"
      className="self-start lg:flex lg:min-h-0 lg:flex-col lg:self-stretch"
    >
      <Card.Header className="flex-row items-start justify-between">
        <Card.Title className="text-accent text-lg font-bold">Principals</Card.Title>
        <AddPrincipalDialog />
      </Card.Header>
      <Card.Content className="lg:flex lg:min-h-0 lg:flex-1 lg:flex-col">
        <ScrollShadow className="lg:min-h-0 lg:flex-1" offset={2}>
          <PrincipalTree selectedId={selectedId} onSelect={onSelect} onDeleted={onDeleted} />
        </ScrollShadow>
        <p className="text-muted text-xs">{caption}</p>
      </Card.Content>
    </Card>
  )
}
