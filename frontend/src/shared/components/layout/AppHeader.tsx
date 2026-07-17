import { Separator } from '@heroui/react'
import { type ReactNode } from 'react'
import { SidebarTrigger } from '@/shared/components/ui/Sidebar'

/** Top bar: sidebar trigger + page title as the left cluster, caller-provided
 *  action buttons (optional) as the right cluster, justified between. */
export function AppHeader({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <header className="flex items-center justify-between gap-4 border-b px-4 py-3">
      <div className="flex min-w-0 items-center gap-3">
        <SidebarTrigger />
        <Separator orientation="vertical" className="h-5" />
        <h1 className="text-foreground truncate text-base font-semibold">{title}</h1>
      </div>
      {children && <div className="flex items-center gap-2">{children}</div>}
    </header>
  )
}
