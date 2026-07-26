import { Button, Card, Skeleton, Tooltip } from '@heroui/react'
import { Database, MousePointerClick } from 'lucide-react'
import { EmptyState } from '@/shared/components/ui/EmptyState'
import { ErrorState } from '@/shared/components/ui/ErrorState'
import { SUMMARY_SEP } from '@/shared/constants'
import { useEffectivePermissions } from '../hooks/usePrincipals'
import { isRevocableGrant, isTraceableOrigin } from '../lib/principal.rules'
import type { EffectiveSource } from '../types/access.type'
import { GrantSourceDialog } from './GrantSourceDialog'
import { OriginChip } from './OriginChip'
import { RevokeGrantButton } from './RevokeGrantButton'

function accessSummary(readableCount: number, inherited: number): string {
  const parts = [`${readableCount} readable in total`]
  if (inherited > 0) parts.push(`${inherited} inherited via groups`)
  return parts.join(SUMMARY_SEP)
}

function GrantRow({
  principalId,
  source,
  onTrace,
}: {
  principalId: string
  source: EffectiveSource
  onTrace?: (source: EffectiveSource) => void
}) {
  const canTrace = onTrace && isTraceableOrigin(source.origin)
  return (
    <div className="flex items-center justify-between gap-2">
      {canTrace ? (
        <Tooltip delay={300}>
          <Button
            variant="ghost"
            onPress={() => onTrace(source)}
            className="hover:text-accent h-auto min-w-0 justify-start bg-transparent p-0 font-normal hover:bg-transparent"
          >
            <span className="truncate text-sm">{source.sourceId}</span>
          </Button>
          <Tooltip.Content>Show this access in the graph</Tooltip.Content>
        </Tooltip>
      ) : (
        <span className="truncate text-sm">{source.sourceId}</span>
      )}
      <div className="flex shrink-0 items-center gap-1">
        <OriginChip origin={source.origin} />
        {isRevocableGrant(source.origin) && (
          <RevokeGrantButton principalId={principalId} sourceId={source.sourceId} />
        )}
      </div>
    </div>
  )
}

/**
 * Every source the selected principal can read — both *direct* grants (which
 * an admin can revoke here) and access *inherited* through group membership
 * (which can't be revoked from this list; it goes away only if the group
 * grant is revoked or the principal leaves the group).
 */
export function PrincipalSourcesPanel({
  principalId,
  onTrace,
}: {
  principalId: string | null
  /** Called when a grant is clicked to trace its path in the graph. Absent when
   *  there is no graph to trace into. */
  onTrace?: (source: EffectiveSource) => void
}) {
  const { data, isPending, isError, error } = useEffectivePermissions(principalId ?? undefined)

  const sources = data?.sources ?? []
  const inherited = sources.filter((s) => s.origin === 'INHERITED').length

  function content() {
    if (!principalId) {
      return (
        <EmptyState
          icon={<MousePointerClick size={28} />}
          description="Select a principal to see its access"
        />
      )
    }
    if (isPending) return <Skeleton className="h-5 w-2/3 rounded" />

    if (isError) return <ErrorState description={(error as Error).message} />

    if (sources.length === 0) {
      return <EmptyState icon={<Database size={28} />} description="No readable sources" />
    }
    return (
      <>
        {sources.map((source) => (
          <GrantRow
            key={source.sourceId}
            principalId={principalId}
            source={source}
            onTrace={onTrace}
          />
        ))}
        <p className="text-muted mt-1 text-xs">{accessSummary(sources.length, inherited)}</p>
      </>
    )
  }

  return (
    <Card className="px-6">
      <Card.Header className="flex-row items-center justify-between">
        <Card.Title className="text-accent text-lg font-bold">Sources</Card.Title>
        <GrantSourceDialog principalId={principalId} />
      </Card.Header>
      <Card.Content className="flex flex-col gap-2">{content()}</Card.Content>
    </Card>
  )
}
