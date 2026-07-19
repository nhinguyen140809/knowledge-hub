import { Button, Input, Label, Modal, TextField } from '@heroui/react'
import { Check, Copy, Plus, TriangleAlert } from 'lucide-react'
import { type FormEvent, useState } from 'react'
import { useIssueCredential } from '../hooks/useCredentials'

/**
 * Issues a credential. The backend returns the raw secret exactly once and never
 * stores it, so the dialog switches to a "copy it now" state instead of closing
 * on success — closing early would lose the only copy that will ever exist.
 */
export function IssueCredentialDialog({ principalId }: { principalId: string | null }) {
  const [isOpen, setOpen] = useState(false)
  const [name, setName] = useState('')
  const [copied, setCopied] = useState(false)
  const issue = useIssueCredential()

  function close() {
    setOpen(false)
    setName('')
    setCopied(false)
    issue.reset()
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    if (principalId) issue.mutate({ principalId, name: name.trim() })
  }

  async function copySecret() {
    if (!issue.data) return
    await navigator.clipboard.writeText(issue.data.secret)
    setCopied(true)
  }

  return (
    <>
      <Button size="sm" variant="secondary" isDisabled={!principalId} onPress={() => setOpen(true)}>
        <Plus size={16} />
        Credential
      </Button>

      <Modal.Backdrop isOpen={isOpen} onOpenChange={(open) => (open ? setOpen(true) : close())}>
        <Modal.Container>
          <Modal.Dialog className="sm:max-w-[460px]">
            <Modal.CloseTrigger />
            <Modal.Header>
              <Modal.Heading>
                {issue.data ? 'Copy this secret now' : `Issue a credential for ${principalId}`}
              </Modal.Heading>
            </Modal.Header>

            {issue.data ? (
              <>
                <Modal.Body className="flex flex-col gap-3">
                  <div className="text-warning flex items-start gap-2 text-sm">
                    <TriangleAlert size={16} className="mt-0.5 shrink-0" />
                    <span>
                      This is the only time the secret is shown. It is not stored and cannot be
                      recovered — reissue the credential if you lose it.
                    </span>
                  </div>
                  <code className="bg-surface-secondary block rounded-lg p-3 font-mono text-xs break-all">
                    {issue.data.secret}
                  </code>
                  <p className="text-muted text-xs">
                    {issue.data.name} · {issue.data.credentialId}
                  </p>
                </Modal.Body>
                <Modal.Footer>
                  <Button variant="secondary" onPress={copySecret}>
                    {copied ? <Check size={16} /> : <Copy size={16} />}
                    {copied ? 'Copied' : 'Copy secret'}
                  </Button>
                  <Button onPress={close}>Done</Button>
                </Modal.Footer>
              </>
            ) : (
              <form onSubmit={onSubmit}>
                <Modal.Body>
                  <TextField value={name} onChange={setName} isRequired>
                    <Label>Name</Label>
                    <Input placeholder="laptop, ci-pipeline" />
                  </TextField>
                  {issue.isError && (
                    <p className="text-danger mt-3 text-sm">{(issue.error as Error).message}</p>
                  )}
                </Modal.Body>
                <Modal.Footer>
                  <Button variant="tertiary" onPress={close}>
                    Cancel
                  </Button>
                  <Button type="submit" isPending={issue.isPending}>
                    Issue
                  </Button>
                </Modal.Footer>
              </form>
            )}
          </Modal.Dialog>
        </Modal.Container>
      </Modal.Backdrop>
    </>
  )
}
