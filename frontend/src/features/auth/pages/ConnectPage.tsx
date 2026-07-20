import { ConnectForm } from '../components/ConnectForm'

/** The "login" screen. There is no session endpoint: the admin API key IS the
 *  credential, so connecting means validating (baseUrl, apiKey) against an
 *  authenticated endpoint, then remembering it as a backend connection. */
export function ConnectPage() {
  return (
    <div className="bg-overlay text-foreground flex min-h-screen items-center justify-center p-4">
      <div className="flex flex-col items-center gap-6">
        <h1 className="text-accent text-4xl font-extrabold">Knowledge Hub</h1>
        <ConnectForm />
      </div>
    </div>
  )
}
