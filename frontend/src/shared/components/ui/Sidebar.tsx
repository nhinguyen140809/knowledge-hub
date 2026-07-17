import { Button, Disclosure, Surface, type ButtonProps } from '@heroui/react'
import { PanelLeft } from 'lucide-react'
import { Activity, useMemo, useState, type ReactNode } from 'react'
import { SidebarContext, useSidebar } from './useSidebar'

/**
 * Reusable, app-agnostic sidebar primitives (structure inspired by shadcn's
 * sidebar): a provider holds the open state, the panel pushes the page aside
 * instead of overlaying it, and menu rows are plain HeroUI Buttons so all
 * colors come from HeroUI variants rather than hand-rolled Tailwind.
 */

export function SidebarProvider({
  defaultOpen = true,
  children,
}: {
  defaultOpen?: boolean
  children: ReactNode
}) {
  const [isOpen, setOpen] = useState(defaultOpen)
  const value = useMemo(() => ({ isOpen, setOpen, toggle: () => setOpen((o) => !o) }), [isOpen])
  return <SidebarContext value={value}>{children}</SidebarContext>
}

/** The panel. Push behaviour: the wrapper animates its width (w-64 ↔ w-0) so
 *  the main page shrinks; the inner Surface keeps a fixed width so the content
 *  never reflows mid-transition. <Activity> hides the content while closed but
 *  preserves its state — an expanded submenu is still expanded on reopen. */
export function Sidebar({ children }: { children: ReactNode }) {
  const { isOpen } = useSidebar()
  return (
    <aside
      className={`shrink-0 overflow-hidden transition-[width] duration-200 ease-out ${
        isOpen ? 'w-64' : 'w-0'
      }`}
    >
      <Activity mode={isOpen ? 'visible' : 'hidden'}>
        <Surface variant="secondary" className="flex h-full w-64 flex-col border-r">
          {children}
        </Surface>
      </Activity>
    </aside>
  )
}

export function SidebarHeader({ children }: { children: ReactNode }) {
  return <div className="flex flex-col gap-3 p-4">{children}</div>
}

export function SidebarContent({ children }: { children: ReactNode }) {
  return <div className="min-h-0 flex-1 overflow-y-auto p-3">{children}</div>
}

export function SidebarFooter({ children }: { children: ReactNode }) {
  return <div className="flex flex-col gap-1 p-3">{children}</div>
}

/** Icon button that opens/closes the sidebar — meant for the app header. */
export function SidebarTrigger() {
  const { isOpen, toggle } = useSidebar()
  return (
    <Button
      isIconOnly
      size="sm"
      variant="ghost"
      aria-label={isOpen ? 'Close sidebar' : 'Open sidebar'}
      onPress={toggle}
    >
      <PanelLeft size={18} />
    </Button>
  )
}

export function SidebarMenu({ children }: { children: ReactNode }) {
  return (
    <nav>
      <ul className="flex flex-col gap-1">{children}</ul>
    </nav>
  )
}

export function SidebarMenuItem({ children }: { children: ReactNode }) {
  return <li>{children}</li>
}

export interface SidebarMenuButtonProps extends ButtonProps {
  isActive?: boolean
}

/** One nav row. HeroUI variants carry the look: tertiary = active row, ghost =
 *  idle. Pass Button's `render` prop to emit a router <Link> instead of a
 *  <button> while keeping HeroUI styling and press behaviour. */
export function SidebarMenuButton({
  isActive = false,
  className,
  ...props
}: SidebarMenuButtonProps) {
  return (
    <Button
      fullWidth
      variant={isActive ? 'tertiary' : 'ghost'}
      className={`justify-start font-normal ${className ?? ''}`}
      {...props}
    />
  )
}

/** Collapsible nav group. The chevron (Disclosure.Indicator) exists only here,
 *  so plain menu items never show an arrow. */
export function SidebarMenuSub({
  label,
  defaultExpanded = false,
  children,
}: {
  label: ReactNode
  defaultExpanded?: boolean
  children: ReactNode
}) {
  return (
    <Disclosure defaultExpanded={defaultExpanded}>
      <Disclosure.Heading>
        <Button slot="trigger" fullWidth variant="ghost" className="justify-start font-normal">
          {label}
          <Disclosure.Indicator className="ml-auto" />
        </Button>
      </Disclosure.Heading>
      <Disclosure.Content>
        <Disclosure.Body>
          <ul className="my-1 ml-4 flex flex-col gap-1 border-l pl-2">{children}</ul>
        </Disclosure.Body>
      </Disclosure.Content>
    </Disclosure>
  )
}
