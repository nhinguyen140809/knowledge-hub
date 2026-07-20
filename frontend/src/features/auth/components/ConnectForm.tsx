import { Button, Form, Input, Label, Surface, TextField } from '@heroui/react'
import { type FormEvent, useState } from 'react'
import { useConnect } from '../hooks/useConnect'

/** The connection form itself: validates (baseUrl, apiKey) against a backend
 *  and remembers it as a connection on success. */
export function ConnectForm() {
  const [label, setLabel] = useState('')
  const [baseUrl, setBaseUrl] = useState('http://localhost:8000')
  const [apiKey, setApiKey] = useState('')
  const connect = useConnect()

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    connect.mutate({ label, baseUrl, apiKey })
  }

  return (
    <Surface className="bg-background text-foreground flex w-full max-w-md min-w-[320px] flex-col gap-3 rounded-3xl p-6">
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
  )
}
