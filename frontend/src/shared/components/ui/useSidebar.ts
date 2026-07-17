import { createContext, useContext } from 'react'

export interface SidebarState {
  isOpen: boolean
  setOpen: (open: boolean) => void
  toggle: () => void
}

/** Shared by <SidebarProvider> (writer) and <Sidebar>/<SidebarTrigger> (readers).
 *  Lives outside Sidebar.tsx so that file only exports components (fast refresh). */
export const SidebarContext = createContext<SidebarState | null>(null)

/** Open/close state of the nearest sidebar; must run under <SidebarProvider>. */
export function useSidebar(): SidebarState {
  const ctx = useContext(SidebarContext)
  if (!ctx) throw new Error('useSidebar must be used within <SidebarProvider>')
  return ctx
}
