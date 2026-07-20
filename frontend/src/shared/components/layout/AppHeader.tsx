import { useHeaderActions } from '@/lib/store/header.store'
import { SidebarTrigger } from '@/shared/components/ui/Sidebar'

/** Top bar: sidebar trigger + page title as the left cluster, the current
 *  page's registered actions (see {@link useSetHeaderActions}) as the right
 *  cluster, justified between. Mounted once in AppLayout — pages reach it
 *  through the header store, not through props. */
export function AppHeader({ title }: { title: string }) {
  const actions = useHeaderActions()
  return (
    <header className="sticky flex items-center justify-between gap-4 px-4 py-3">
      <div className="flex min-w-0 items-center gap-3">
        <SidebarTrigger />
        <h1 className="text-foreground truncate text-xl font-semibold">{title}</h1>
      </div>
      {actions && <div className="flex items-center gap-2">{actions}</div>}
    </header>
  )
}
