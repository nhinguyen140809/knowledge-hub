import { AlertDialog, Button, Card, Chip, Skeleton } from '@heroui/react'
import { Ban } from 'lucide-react'
import { formatTimestamp } from '@/shared/lib/datetime.utils'
import { useCredentials, useRevokeCredential } from '../hooks/useCredentials'
import { IssueCredentialDialog } from './IssueCredentialDialog'

/** Revoking is a soft-delete that immediately breaks whatever is using the key,
 *  so it is confirmed rather than fired on a single click. */
function RevokeButton({ credentialId, name }: { credentialId: string; name: string }) {
  const revoke = useRevokeCredential()
  return (
    <AlertDialog>
      <Button isIconOnly size="sm" variant="ghost" aria-label={`Revoke ${name}`}>
        <Ban size={15} />
      </Button>
      <AlertDialog.Backdrop>
        <AlertDialog.Container>
          <AlertDialog.Dialog className="sm:max-w-[420px]">
            <AlertDialog.CloseTrigger />
            <AlertDialog.Header>
              <AlertDialog.Icon status="danger">
                <Ban className="size-5" />
              </AlertDialog.Icon>
              <AlertDialog.Heading>Revoke this credential?</AlertDialog.Heading>
            </AlertDialog.Header>
            <AlertDialog.Body>
              <p>
                Requests using <strong>{name}</strong> start failing authentication immediately.
                This cannot be undone — issue a new credential instead.
              </p>
            </AlertDialog.Body>
            <AlertDialog.Footer>
              <Button slot="close" variant="tertiary">
                Cancel
              </Button>
              <Button
                slot="close"
                variant="danger"
                isPending={revoke.isPending}
                onPress={() => revoke.mutate(credentialId)}
              >
                Revoke
              </Button>
            </AlertDialog.Footer>
          </AlertDialog.Dialog>
        </AlertDialog.Container>
      </AlertDialog.Backdrop>
    </AlertDialog>
  )
}

/** Credentials issued to the selected principal. Secrets are never listed — the
 *  raw secret exists only in the response that issued it. */
export function PrincipalCredentialsPanel({ principalId }: { principalId: string | null }) {
  const { data, isPending } = useCredentials(principalId ?? undefined)

  return (
    <Card>
      <Card.Header className="flex-row items-center justify-between">
        <Card.Title>Credentials</Card.Title>
        <IssueCredentialDialog principalId={principalId} />
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
              <div className="flex shrink-0 items-center gap-1">
                <Chip size="sm" variant="soft" color={credential.revoked ? 'danger' : 'success'}>
                  {credential.revoked ? 'revoked' : 'active'}
                </Chip>
                {!credential.revoked && (
                  <RevokeButton credentialId={credential.credentialId} name={credential.name} />
                )}
              </div>
            </div>
          ))}
      </Card.Content>
    </Card>
  )
}
