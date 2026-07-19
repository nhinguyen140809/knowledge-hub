import { Button, Card, Input, Label, TextField } from '@heroui/react'
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
      <Card className="w-full max-w-md">
        <Card.Header>
          <Card.Title>Connect to a backend</Card.Title>
          <Card.Description>
            Nhập URL và admin API key của một instance Knowledge Hub.
          </Card.Description>
        </Card.Header>
        <form onSubmit={onSubmit}>
          <Card.Content className="flex flex-col gap-4">
            <TextField value={label} onChange={setLabel}>
              <Label>Tên gợi nhớ (tuỳ chọn)</Label>
              <Input placeholder="Production, Local dev..." />
            </TextField>
            <TextField value={baseUrl} onChange={setBaseUrl} type="url" isRequired>
              <Label>Base URL</Label>
              <Input placeholder="http://localhost:8000" />
            </TextField>
            <TextField value={apiKey} onChange={setApiKey} type="password" isRequired>
              <Label>Admin API key</Label>
              <Input placeholder="Bearer token" />
            </TextField>
            {connect.isError && (
              <p className="text-danger text-sm">
                Kết nối thất bại: {(connect.error as Error).message}
              </p>
            )}
          </Card.Content>
          <Card.Footer>
            <Button type="submit" isDisabled={connect.isPending} fullWidth>
              {connect.isPending ? 'Đang kiểm tra...' : 'Kết nối'}
            </Button>
          </Card.Footer>
        </form>
      </Card>
    </div>
  )
}
