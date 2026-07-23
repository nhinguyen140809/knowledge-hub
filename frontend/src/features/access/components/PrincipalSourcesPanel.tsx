import { Button, Card, Chip, Skeleton } from '@heroui/react'
import { Database, MousePointerClick, CircleMinus } from 'lucide-react'
import { ConfirmDialog } from '@/shared/components/ui/ConfirmDialog'
import { EmptyState } from '@/shared/components/ui/EmptyState'
import { ErrorState } from '@/shared/components/ui/ErrorState'
import { useEffectivePermissions } from '../hooks/usePrincipals'
import { useRevokeSources } from '../hooks/useGrants'
import type { GrantOrigin } from '../types/access.type'
import { GrantSourceDialog } from './GrantSourceDialog'

/** Each source arrives already tagged with its origin; this only maps the tag
 *  to a look. Only DIRECT is revocable from this panel. */
const GRANT_ORIGIN_CONFIG: Record<
  GrantOrigin,
  { color: 'default' | 'accent' | 'warning' | 'danger'; label: string }
> = {
  DIRECT: { color: 'default', label: 'direct' },
  INHERITED: { color: 'accent', label: 'inherited' },
  ADMIN: { color: 'danger', label: 'admin' },
  POLICY: { color: 'warning', label: 'policy' },
}

function accessSummary(readableCount: number, inherited: number): string {
  const parts = [`${readableCount} readable in total`]
  if (inherited > 0) parts.push(`· ${inherited} inherited via groups`)
  return parts.join(' ')
}

/** Revoking a direct grant removes read access immediately, so it goes
 *  through the same confirm step as deleting a source or revoking a
 *  credential. */
function RevokeGrantButton({ principalId, sourceId }: { principalId: string; sourceId: string }) {
  const revoke = useRevokeSources()
  return (
    <ConfirmDialog
      trigger={
        <Button isIconOnly size="sm" variant="ghost" aria-label={`Revoke access to ${sourceId}`}>
          <CircleMinus size={14} />
        </Button>
      }
      icon={<CircleMinus className="size-5" />}
      heading="Revoke this grant?"
      message={
        <p>
          <strong>{principalId}</strong> loses direct read access to <strong>{sourceId}</strong>{' '}
          immediately. It may still be reachable through a group grant or the default policy.
        </p>
      }
      confirmButton={
        <Button
          slot="close"
          variant="danger"
          isPending={revoke.isPending}
          onPress={() => revoke.mutate({ principalId, sourceIds: [sourceId] })}
        >
          Revoke
        </Button>
      }
    />
  )
}

function GrantRow({
  principalId,
  sourceId,
  origin,
}: {
  principalId: string
  sourceId: string
  origin: GrantOrigin
}) {
  const config = GRANT_ORIGIN_CONFIG[origin]
  return (
    <div className="flex items-center justify-between gap-2">
      <span className="truncate text-sm">{sourceId}</span>
      <div className="flex shrink-0 items-center gap-1">
        <Chip size="sm" variant="soft" color={config.color}>
          {config.label}
        </Chip>
        {origin === 'DIRECT' && <RevokeGrantButton principalId={principalId} sourceId={sourceId} />}
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
export function PrincipalSourcesPanel({ principalId }: { principalId: string | null }) {
  const { data, isPending, isError, error } = useEffectivePermissions(principalId ?? undefined)

  const sources = data?.sources ?? []
  const inherited = sources.filter((s) => s.origin === 'INHERITED').length

  function content() {
    if (!principalId) {
      return (
        <EmptyState
          icon={<MousePointerClick size={28} />}
          description="Select a principal to see its access."
        />
      )
    }
    if (isPending) return <Skeleton className="h-5 w-2/3 rounded" />

    if (isError) return <ErrorState description={(error as Error).message} />

    if (sources.length === 0) {
      return <EmptyState icon={<Database size={28} />} description="No readable sources." />
    }
    return (
      <>
        {sources.map((source) => (
          <GrantRow
            key={source.sourceId}
            principalId={principalId}
            sourceId={source.sourceId}
            origin={source.origin}
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
