import { Tabs } from '@heroui/react'
import { useState } from 'react'
import { type Connection, useConnectionStore } from '@/lib/store/connections.store'
import { useFormReducer } from '@/shared/hooks/useFormReducer'
import { ConnectForm, type ConnectFormState } from '../components/ConnectForm'

const EMPTY: ConnectFormState = { label: '', baseUrl: 'http://localhost:8000', apiKey: '' }

type ConnectView = 'new' | 'reconnect'

/**
 * The "login" screen. There is no session endpoint: the admin API key IS the
 * credential, so connecting means validating (baseUrl, apiKey) against an
 * authenticated endpoint, then remembering it as a backend connection.
 *
 * With no connections saved yet, there is nothing to reconnect to, so the
 * screen is just the bare "New connection" form — no tab bar for a single
 * tab. Once a connection exists, it becomes a login/register-style split:
 * "New connection" stays a blank form, "Reconnect" is the same form with a
 * "Saved connections" picker built in, picking one prefills label/baseUrl
 * there. Each tab has its own independent form state. `addConnection`
 * matches by baseUrl, so submitting reconnects that entry rather than
 * creating a new one.
 */
export function ConnectPage() {
  const connections = useConnectionStore((s) => s.connections)
  const activeId = useConnectionStore((s) => s.activeId)
  const [newForm, setNewField] = useFormReducer(EMPTY)
  const [reconnectForm, setReconnectField, replaceReconnect] = useFormReducer(EMPTY)
  const [view, setView] = useState<ConnectView>(connections.length > 0 ? 'reconnect' : 'new')

  function pickSaved(connection: Connection) {
    replaceReconnect({ label: connection.label, baseUrl: connection.baseUrl, apiKey: '' })
  }

  if (connections.length === 0) {
    return (
      <div className="bg-overlay text-foreground flex min-h-screen flex-col items-center justify-center gap-6 p-4">
        <h1 className="text-accent text-4xl font-extrabold">Knowledge Hub</h1>
        <ConnectForm form={newForm} setField={setNewField} />
      </div>
    )
  }

  return (
    <div className="bg-overlay text-foreground flex min-h-screen flex-col items-center justify-center gap-6 p-4">
      <h1 className="text-accent text-4xl font-extrabold">Knowledge Hub</h1>

      <Tabs
        selectedKey={view}
        onSelectionChange={(key) => setView(key as ConnectView)}
        className="w-full max-w-md min-w-[320px]"
      >
        <Tabs.ListContainer>
          <Tabs.List aria-label="Connect views">
            <Tabs.Tab id="new">
              New connection
              <Tabs.Indicator />
            </Tabs.Tab>
            <Tabs.Tab id="reconnect">
              Reconnect
              <Tabs.Indicator />
            </Tabs.Tab>
          </Tabs.List>
        </Tabs.ListContainer>

        <Tabs.Panel id="new" className="pt-4">
          <ConnectForm form={newForm} setField={setNewField} />
        </Tabs.Panel>

        <Tabs.Panel id="reconnect" className="pt-4">
          <ConnectForm
            form={reconnectForm}
            setField={setReconnectField}
            savedConnections={connections}
            activeId={activeId}
            onPickConnection={pickSaved}
          />
        </Tabs.Panel>
      </Tabs>
    </div>
  )
}
