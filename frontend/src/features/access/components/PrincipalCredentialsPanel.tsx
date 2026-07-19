import { Button, Card, Chip, Skeleton } from '@heroui/react'
import { Plus } from 'lucide-react'
import { formatTimestamp } from '@/shared/lib/datetime.utils'
import { useCredentials } from '../hooks/useCredentials'

/** Credentials issued to the selected principal. Secrets are never listed — the
 *  raw secret exists only in the response that issued it. */
export function PrincipalCredentialsPanel({ principalId }: { principalId: string | null }) {
  const { data, isPending } = useCredentials(principalId ?? undefined)

  return (
    <Card>
      <Card.Header className="flex-row items-center justify-between">
        <Card.Title>Credentials</Card.Title>
        <Button size="sm" variant="secondary" isDisabled={!principalId}>
          <Plus size={16} />
          Credential
        </Button>
      </Card.Header>
      <Card.Content className="flex flex-col gap-3">
        {!principalId && <p className="text-muted text-sm">Select a principal to see its keys.</p>}

        {principalId && isPending && (
          <>
            <Skeleton className="h-10 w-full rounded" />
            <Skeleton className="h-10 w-full rounded" />
          </>
        )}

        {principalId && data && data.length === 0 && (
          <p className="text-muted text-sm">No credentials issued.</p>
        )}

        {principalId &&
          data?.map((credential) => (
            <div
              key={credential.credentialId}
              className="flex items-start justify-between gap-3 border-b pb-2 last:border-b-0 last:pb-0"
            >
              <div className="flex min-w-0 flex-col">
                <span className="truncate text-sm font-medium">{credential.name}</span>
                <span className="text-muted font-mono text-xs">{credential.credentialId}</span>
                <span className="text-muted text-xs">
                  created {formatTimestamp(new Date(credential.createdAt))}
                  {credential.lastUsedAt
                    ? ` · last used ${formatTimestamp(new Date(credential.lastUsedAt))}`
                    : ' · never used'}
                </span>
              </div>
              <Chip size="sm" variant="soft" color={credential.revoked ? 'danger' : 'success'}>
                {credential.revoked ? 'revoked' : 'active'}
              </Chip>
            </div>
          ))}
      </Card.Content>
    </Card>
  )
}
