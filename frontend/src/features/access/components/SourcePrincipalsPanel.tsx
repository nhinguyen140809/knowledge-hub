import { Button, Card, Skeleton, Tooltip } from '@heroui/react'
import { Users } from 'lucide-react'
import { EmptyState } from '@/shared/components/ui/EmptyState'
import { ErrorState } from '@/shared/components/ui/ErrorState'
import { SUMMARY_SEP } from '@/shared/constants'
import { useSourcePrincipals } from '../hooks/useSourceAccess'
import type { SourcePrincipal } from '../types/access.type'
import { OriginChip } from './OriginChip'

function accessSummary(count: number, inherited: number): string {
  const parts = [`${count} ${count === 1 ? 'principal can' : 'principals can'} read this source`]
  if (inherited > 0) parts.push(`${inherited} inherited via groups`)
  return parts.join(SUMMARY_SEP)
}

function PrincipalRow({
  principal,
  onSelect,
}: {
  principal: SourcePrincipal
  onSelect?: (principalId: string) => void
}) {
  return (
    <div className="flex items-center justify-between gap-2">
      {onSelect ? (
        <Tooltip delay={300}>
          <Button
            variant="ghost"
            onPress={() => onSelect(principal.principalId)}
            className="hover:text-accent h-auto min-w-0 justify-start bg-transparent p-0 font-normal hover:bg-transparent"
          >
            <span className="truncate text-sm">{principal.principalId}</span>
          </Button>
          <Tooltip.Content>Open in Access</Tooltip.Content>
        </Tooltip>
      ) : (
        <span className="truncate text-sm">{principal.principalId}</span>
      )}
      <OriginChip origin={principal.origin} />
    </div>
  )
}

/**
 * Every principal that can read this source, with how its access arrived —
 * direct grant, inherited through a group, admin role bypass, or the default
 * policy. The inverse of a principal's own Sources panel.
 */
export function SourcePrincipalsPanel({
  sourceId,
  onSelectPrincipal,
}: {
  sourceId: string
  /** Called when a principal is clicked, to open it on the Access page. */
  onSelectPrincipal?: (principalId: string) => void
}) {
  const { data, isPending, isError, error } = useSourcePrincipals(sourceId)

  const principals = data?.principals ?? []
  const inherited = principals.filter((p) => p.origin === 'INHERITED').length

  function content() {
    if (isPending) return <Skeleton className="h-5 w-2/3 rounded" />
    if (isError) return <ErrorState description={(error as Error).message} />
    if (principals.length === 0) {
      return (
        <EmptyState icon={<Users size={28} />} description="No principal can read this source" />
      )
    }
    return (
      <>
        {principals.map((principal) => (
          <PrincipalRow
            key={principal.principalId}
            principal={principal}
            onSelect={onSelectPrincipal}
          />
        ))}
        <p className="text-muted mt-1 text-xs">{accessSummary(principals.length, inherited)}</p>
      </>
    )
  }

  return (
    <Card className="px-6">
      <Card.Header>
        <Card.Title className="text-accent text-lg font-bold">Who can access this</Card.Title>
      </Card.Header>
      <Card.Content className="flex flex-col gap-2">{content()}</Card.Content>
    </Card>
  )
}
