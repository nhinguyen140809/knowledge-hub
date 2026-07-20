import { Button, Input, Label, TextField, Surface, Form } from '@heroui/react'
import { type FormEvent, useState } from 'react'
import { useConnect } from '../hooks/useConnect'

/** The "login" screen. There is no session endpoint: the admin API key IS the
 *  credential, so connecting means validating (baseUrl, apiKey) against an
 *  authenticated endpoint, then remembering it as a backend connection. */
export function ConnectPage() {
  const [label, setLabel] = useState('')
  const [baseUrl, setBaseUrl] = useState('http://localhost:8000')
  const [apiKey, setApiKey] = useState('')
  const connect = useConnect()

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    connect.mutate({ label, baseUrl, apiKey })
  }

  return (
    <div className="bg-background text-foreground flex min-h-screen items-center justify-center p-4">
      <div className="flex flex-col items-center gap-6">
      <h1 className="text-accent text-4xl font-bold">Knowledge Hub</h1>
      <Surface className="flex w-full max-w-md min-w-[320px] flex-col gap-3 rounded-3xl p-6">
        <h3 className="text-foreground text-xl font-semibold">Connect to a backend</h3>
        <p className="text-muted-foreground text-sm">
          Enter the URL and admin API key of a Knowledge Hub instance
        </p>

        <Form className="flex flex-col gap-4" onSubmit={onSubmit}>
          <TextField value={label} onChange={setLabel}>
            <Label>Label (optional)</Label>
            <Input placeholder="Production, Local dev..." />
          </TextField>
          <TextField value={baseUrl} onChange={setBaseUrl} type="url" name="url" isRequired>
            <Label>Base URL</Label>
            <Input placeholder="http://localhost:8000" />
          </TextField>
          <TextField value={apiKey} onChange={setApiKey} type="password" name="apiKey" isRequired>
            <Label>Admin API key</Label>
            <Input placeholder="Bearer token" />
          </TextField>
          {connect.isError && (
            <p className="text-danger text-sm">
              Connection failed: {(connect.error as Error).message}
            </p>
          )}
          <div className="flex-1">
            <Button type="submit" isDisabled={connect.isPending} fullWidth>
              {connect.isPending ? 'Checking...' : 'Connect'}
            </Button>
          </div>
        </Form>
      </Surface>
      </div>
    </div>
  )
}
