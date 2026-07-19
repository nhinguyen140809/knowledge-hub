import { Button, Card, Chip, Skeleton } from '@heroui/react'
import { Plus } from 'lucide-react'
import { useEffectivePermissions } from '../hooks/usePrincipals'
import { useGrants } from '../hooks/useGrants'

/**
 * Sources the selected principal can read. The list shows *direct* grants —
 * the ones an admin can add or remove here — while the footer reports the
 * resolved total, which also counts access inherited through groups.
 */
export function PrincipalSourcesPanel({ principalId }: { principalId: string | null }) {
  const grants = useGrants(principalId ?? undefined)
  const permissions = useEffectivePermissions(principalId ?? undefined)

  const inherited = permissions.data
    ? permissions.data.readableSources.length - (grants.data?.length ?? 0)
    : 0

  return (
    <Card>
      <Card.Header className="flex-row items-center justify-between">
        <Card.Title>Sources</Card.Title>
        <Button size="sm" variant="secondary" isDisabled={!principalId}>
          <Plus size={16} />
          Source
        </Button>
      </Card.Header>
      <Card.Content className="flex flex-col gap-2">
        {!principalId && (
          <p className="text-muted text-sm">Select a principal to see its access.</p>
        )}

        {principalId && grants.isPending && <Skeleton className="h-5 w-2/3 rounded" />}

        {principalId && grants.data && grants.data.length === 0 && (
          <p className="text-muted text-sm">No direct grants.</p>
        )}

        {principalId &&
          grants.data?.map((sourceId) => (
            <div key={sourceId} className="flex items-center justify-between gap-2">
              <span className="truncate font-mono text-sm">{sourceId}</span>
              <Chip size="sm" variant="soft">
                direct
              </Chip>
            </div>
          ))}

        {principalId && permissions.data && (
          <p className="text-muted mt-1 text-xs">
            {permissions.data.readableSources.length} readable in total
            {inherited > 0 && ` — ${inherited} inherited via groups`} · default policy{' '}
            {permissions.data.defaultPolicy}
          </p>
        )}
      </Card.Content>
    </Card>
  )
}
