import { Button, Form, Input, Label, ListBox, Select, Surface, TextField } from '@heroui/react'
import { type FormEvent } from 'react'
import { type Connection } from '@/lib/store/connections.store'
import { useConnect } from '../hooks/useConnect'

export interface ConnectFormState {
  label: string
  baseUrl: string
  apiKey: string
}

interface ConnectFormProps {
  form: ConnectFormState
  setField: <K extends keyof ConnectFormState>(key: K) => (value: ConnectFormState[K]) => void
  /** When given, a "Saved connections" picker renders above the fields —
   *  picking one calls `onPickConnection` so the caller can prefill this
   *  same form (see the "Reconnect" tab on {@link ConnectPage}). */
  savedConnections?: Connection[]
  activeId?: string | null
  onPickConnection?: (connection: Connection) => void
}

/** The connection form itself: validates (baseUrl, apiKey) against a backend
 *  and remembers it as a connection on success. Controlled from the parent —
 *  {@link ConnectPage} owns the field state so picking a saved connection can
 *  prefill label/baseUrl into this same form. */
export function ConnectForm({
  form,
  setField,
  savedConnections,
  activeId,
  onPickConnection,
}: ConnectFormProps) {
  const connect = useConnect()

  function onSubmit(e: FormEvent) {
    e.preventDefault()
    connect.mutate(form)
  }

  return (
    <Surface className="bg-background text-foreground flex w-full max-w-md min-w-[320px] flex-col gap-3 rounded-3xl p-6">
      <h3 className="text-foreground text-xl font-semibold">Connect to a backend</h3>

      <Form className="flex flex-col gap-4" onSubmit={onSubmit}>
        {savedConnections && savedConnections.length > 0 && (
          <Select
            placeholder="Pick a saved connection"
            onChange={(key) => {
              const connection = savedConnections.find((c) => c.id === key)
              if (connection) onPickConnection?.(connection)
            }}
          >
            <Label>Saved connections</Label>
            <Select.Trigger>
              <Select.Value />
              <Select.Indicator />
            </Select.Trigger>
            <Select.Popover>
              <ListBox>
                {savedConnections.map((c) => (
                  <ListBox.Item key={c.id} id={c.id} textValue={c.label}>
                    <div className="flex flex-col">
                      <span className="flex items-center gap-2">
                        {c.label}
                        {c.id === activeId && <span className="text-muted text-xs">(current)</span>}
                      </span>
                      <span className="text-muted font-mono text-xs">{c.baseUrl}</span>
                    </div>
                    <ListBox.ItemIndicator />
                  </ListBox.Item>
                ))}
              </ListBox>
            </Select.Popover>
          </Select>
        )}
        <TextField value={form.label} onChange={setField('label')}>
          <Label>Label (optional)</Label>
          <Input placeholder="Production, Local dev..." />
        </TextField>
        <TextField
          value={form.baseUrl}
          onChange={setField('baseUrl')}
          type="url"
          name="url"
          isRequired
        >
          <Label>Base URL</Label>
          <Input placeholder="http://localhost:8000" />
        </TextField>
        <TextField
          value={form.apiKey}
          onChange={setField('apiKey')}
          type="password"
          name="apiKey"
          isRequired
        >
          <Label>Admin API key</Label>
          <Input placeholder="Bearer token" />
        </TextField>
        <div className="flex-1">
          <Button type="submit" isDisabled={connect.isPending} fullWidth>
            {connect.isPending ? 'Checking...' : 'Connect'}
          </Button>
        </div>
      </Form>
    </Surface>
  )
}
