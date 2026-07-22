import { Button, Input, Label, Modal, TextField } from '@heroui/react'
import { Check, Copy, Plus, TriangleAlert } from 'lucide-react'
import { type FormEvent, useState } from 'react'
import { useIssueCredential } from '../hooks/useCredentials'
import type { IssuedCredential } from '../types/access.type'

interface IssueFormProps {
  isPending: boolean
  onSubmit: (name: string) => void
  onCancel: () => void
}

/** The "not issued yet" state: just a name, nothing about the secret exists. */
function IssueForm({ isPending, onSubmit, onCancel }: IssueFormProps) {
  const [name, setName] = useState('')

  function handleSubmit(e: FormEvent) {
    e.preventDefault()
    onSubmit(name.trim())
  }

  return (
    <form onSubmit={handleSubmit}>
      <Modal.Body>
        <TextField value={name} onChange={setName} isRequired>
          <Label>Name</Label>
          <Input placeholder="laptop, ci-pipeline" />
        </TextField>
      </Modal.Body>
      <Modal.Footer>
        <Button variant="tertiary" onPress={onCancel}>
          Cancel
        </Button>
        <Button type="submit" isPending={isPending}>
          Issue
        </Button>
      </Modal.Footer>
    </form>
  )
}

interface RevealSecretProps {
  credential: IssuedCredential
  onDone: () => void
}

/** The "just issued" state: the raw secret, shown exactly once — the backend
 *  never stores it, so this is the only chance to copy it. */
function RevealSecret({ credential, onDone }: RevealSecretProps) {
  const [copied, setCopied] = useState(false)

  async function copySecret() {
    await navigator.clipboard.writeText(credential.secret)
    setCopied(true)
  }

  return (
    <>
      <Modal.Body className="flex flex-col gap-3">
        <div className="text-warning flex items-start gap-2 text-sm">
          <TriangleAlert size={16} className="mt-0.5 shrink-0" />
          <span>
            This is the only time the secret is shown. It is not stored and cannot be recovered,
            reissue the credential if you lose it.
          </span>
        </div>
        <code className="bg-surface-secondary text-foreground block rounded-lg p-3 text-sm break-all">
          {credential.secret}
        </code>
        <p className="text-muted text-xs">
          {credential.name} · {credential.credentialId}
        </p>
      </Modal.Body>
      <Modal.Footer>
        <Button variant="secondary" onPress={copySecret}>
          {copied ? <Check size={16} /> : <Copy size={16} />}
          {copied ? 'Copied' : 'Copy secret'}
        </Button>
        <Button onPress={onDone}>Done</Button>
      </Modal.Footer>
    </>
  )
}

/**
 * Issues a credential. The backend returns the raw secret exactly once and never
 * stores it, so the dialog switches to a "copy it now" state instead of closing
 * on success — closing early would lose the only copy that will ever exist.
 */
export function IssueCredentialDialog({ principalId }: { principalId: string | null }) {
  const [isOpen, setOpen] = useState<boolean>(false)
  const issue = useIssueCredential()

  function close() {
    setOpen(false)
    issue.reset()
  }

  return (
    <>
      <Button size="sm" variant="primary" isDisabled={!principalId} onPress={() => setOpen(true)}>
        <Plus size={16} />
        Credential
      </Button>

      <Modal.Backdrop isOpen={isOpen} onOpenChange={(open) => (open ? setOpen(true) : close())}>
        <Modal.Container>
          <Modal.Dialog className="sm:max-w-[460px]">
            <Modal.CloseTrigger />
            <Modal.Header>
              <Modal.Heading className="mb-2">
                {issue.data ? 'Copy this secret now' : `Issue a credential for ${principalId}`}
              </Modal.Heading>
            </Modal.Header>

            {issue.data ? (
              <RevealSecret credential={issue.data} onDone={close} />
            ) : (
              <IssueForm
                isPending={issue.isPending}
                onCancel={close}
                onSubmit={(name) => principalId && issue.mutate({ principalId, name })}
              />
            )}
          </Modal.Dialog>
        </Modal.Container>
      </Modal.Backdrop>
    </>
  )
}
