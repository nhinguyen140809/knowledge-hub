import { Button, Card, Chip, Skeleton } from '@heroui/react'
import { Ban, KeyRound, MousePointerClick } from 'lucide-react'
import { ConfirmDialog } from '@/shared/components/ui/ConfirmDialog'
import { EmptyState } from '@/shared/components/ui/EmptyState'
import { ErrorState } from '@/shared/components/ui/ErrorState'
import { IconButton } from '@/shared/components/ui/IconButton'
import { SUMMARY_SEP } from '@/shared/constants'
import { formatTimestamp } from '@/shared/lib/datetime.utils'
import { useCredentials, useRevokeCredential } from '../hooks/useCredentials'
import { canRevokeCredential } from '../lib/principal.rules'
import type { Credential } from '../types/access.type'
import { IssueCredentialDialog } from './IssueCredentialDialog'

const CREDENTIAL_STATUS_CONFIG = {
  revoked: { color: 'danger', label: 'revoked' },
  active: { color: 'success', label: 'active' },
} as const

function credentialSummary(credential: Credential): string {
  const created = `created ${formatTimestamp(new Date(credential.createdAt))}`
  const lastUsed = credential.lastUsedAt
    ? `last used ${formatTimestamp(new Date(credential.lastUsedAt))}`
    : 'never used'
  return `${created}${SUMMARY_SEP}${lastUsed}`
}

/** Revoking is a soft-delete that immediately breaks whatever is using the key,
 *  so it is confirmed rather than fired on a single click. */
function RevokeButton({ credentialId, name }: { credentialId: string; name: string }) {
  const revoke = useRevokeCredential()
  return (
    <ConfirmDialog
      trigger={
        <IconButton tooltip={`Revoke credential`} size="sm" variant="ghost">
          <Ban size={15} />
        </IconButton>
      }
      icon={<Ban className="size-5" />}
      heading="Revoke this credential?"
      message={
        <p>
          Requests using <strong>{name}</strong> start failing authentication immediately. This
          cannot be undone, issue a new credential instead.
        </p>
      }
      confirmButton={
        <Button
          slot="close"
          variant="danger"
          isPending={revoke.isPending}
          onPress={() => revoke.mutate(credentialId)}
        >
          Revoke
        </Button>
      }
    />
  )
}

function CredentialRow({ credential }: { credential: Credential }) {
  const status = CREDENTIAL_STATUS_CONFIG[credential.revoked ? 'revoked' : 'active']
  return (
    <div className="flex items-start justify-between gap-3 border-b pb-2 last:border-b-0 last:pb-0">
      <div className="flex min-w-0 flex-col gap-1">
        <span className="truncate text-sm font-medium">{credential.name}</span>
        <span className="text-muted text-xs">{credentialSummary(credential)}</span>
      </div>
      <div className="flex shrink-0 items-center gap-1">
        <Chip size="sm" variant="soft" color={status.color}>
          {status.label}
        </Chip>
        {canRevokeCredential(credential) && (
          <RevokeButton credentialId={credential.credentialId} name={credential.name} />
        )}
      </div>
    </div>
  )
}

/** Credentials issued to the selected principal. Secrets are never listed — the
 *  raw secret exists only in the response that issued it. */
export function PrincipalCredentialsPanel({ principalId }: { principalId: string | null }) {
  const { data, isPending, isError, error } = useCredentials(principalId ?? undefined)

  function content() {
    if (!principalId) {
      return (
        <EmptyState
          icon={<MousePointerClick size={28} />}
          description="Select a principal to see its keys"
        />
      )
    }
    if (isPending) {
      return (
        <>
          <Skeleton className="h-10 w-full rounded" />
          <Skeleton className="h-10 w-full rounded" />
        </>
      )
    }

    if (isError) return <ErrorState description={(error as Error).message} />

    if (!data || data.length === 0) {
      return <EmptyState icon={<KeyRound size={28} />} description="No credentials issued" />
    }

    return data.map((credential) => (
      <CredentialRow key={credential.credentialId} credential={credential} />
    ))
  }

  return (
    <Card className="px-6">
      <Card.Header className="flex-row items-center justify-between">
        <Card.Title className="text-accent text-lg font-bold">Credentials</Card.Title>
        <IssueCredentialDialog principalId={principalId} />
      </Card.Header>
      <Card.Content className="flex flex-col gap-3">{content()}</Card.Content>
    </Card>
  )
}
