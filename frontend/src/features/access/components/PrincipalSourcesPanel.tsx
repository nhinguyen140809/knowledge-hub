import { Button, Card, Chip, Skeleton } from '@heroui/react'
import { Database, MousePointerClick, Plus, X } from 'lucide-react'
import { ConfirmDialog } from '@/shared/components/ui/ConfirmDialog'
import { EmptyState } from '@/shared/components/ui/EmptyState'
import { ErrorState } from '@/shared/components/ui/ErrorState'
import { useEffectivePermissions } from '../hooks/usePrincipals'
import { useDirectGrants, useRevokeSources } from '../hooks/useGrants'

const GRANT_ORIGIN_CONFIG = {
  direct: { color: 'default', label: 'direct' },
  inherited: { color: 'accent', label: 'inherited' },
} as const

function accessSummary(readableCount: number, inherited: number, defaultPolicy: string): string {
  const parts = [`${readableCount} readable in total`]
  if (inherited > 0) parts.push(`· ${inherited} inherited via groups`)
  parts.push(`· default policy ${defaultPolicy}`)
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
          <X size={14} />
        </Button>
      }
      icon={<X className="size-5" />}
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
  isDirect,
}: {
  principalId: string
  sourceId: string
  isDirect: boolean
}) {
  const origin = GRANT_ORIGIN_CONFIG[isDirect ? 'direct' : 'inherited']
  return (
    <div className="flex items-center justify-between gap-2">
      <span className="truncate text-sm">{sourceId}</span>
      <div className="flex shrink-0 items-center gap-1">
        <Chip size="sm" variant="soft" color={origin.color}>
          {origin.label}
        </Chip>
        {isDirect && <RevokeGrantButton principalId={principalId} sourceId={sourceId} />}
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
  const grants = useDirectGrants(principalId ?? undefined)
  const permissions = useEffectivePermissions(principalId ?? undefined)

  const isLoading = grants.isPending || permissions.isPending
  const directSet = new Set(grants.data ?? [])
  const readable = permissions.data?.readableSources ?? []
  const inherited = readable.length - directSet.size

  return (
    <Card className="px-6">
      <Card.Header className="flex-row items-center justify-between">
        <Card.Title className="text-accent text-lg font-bold">Sources</Card.Title>
        <Button size="sm" variant="primary" isDisabled={!principalId}>
          <Plus size={16} />
          Source
        </Button>
      </Card.Header>
      <Card.Content className="flex flex-col gap-2">
        {!principalId && (
          <EmptyState
            icon={<MousePointerClick size={28} />}
            description="Select a principal to see its access."
          />
        )}

        {principalId && isLoading && <Skeleton className="h-5 w-2/3 rounded" />}

        {principalId && !isLoading && permissions.isError && (
          <ErrorState description={(permissions.error as Error).message} />
        )}

        {principalId && !isLoading && readable.length === 0 && (
          <EmptyState icon={<Database size={28} />} description="No readable sources." />
        )}

        {principalId &&
          !isLoading &&
          readable.map((sourceId) => (
            <GrantRow
              key={sourceId}
              principalId={principalId}
              sourceId={sourceId}
              isDirect={directSet.has(sourceId)}
            />
          ))}

        {principalId && permissions.data && (
          <p className="text-muted mt-1 text-xs">
            {accessSummary(readable.length, inherited, permissions.data.defaultPolicy)}
          </p>
        )}
      </Card.Content>
    </Card>
  )
}
