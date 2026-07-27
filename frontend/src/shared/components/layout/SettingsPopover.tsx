import { Button, Popover } from '@heroui/react'
import { Settings } from 'lucide-react'
import { type ComponentProps, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ThemeToggle } from '@/shared/components/theme/ThemeToggle'
import { SETTINGS_ITEMS } from '@/shared/constants'

/** One row inside the popover — same look as a sidebar row (ghost, full-width,
 *  left-aligned), written here directly rather than reusing SidebarMenuButton:
 *  that component means "a row of the sidebar's own nav," which this isn't. */
function PopoverRow(props: ComponentProps<typeof Button>) {
  return <Button fullWidth variant="ghost" className="justify-start font-normal" {...props} />
}

/**
 * App-level preferences behind one footer trigger: toggle the theme in place,
 * or open one of SETTINGS_ITEMS's pages (today, just the connections
 * registry — add still lives at /connect, this is where a stale one gets
 * forgotten). Controlled open state so picking a page closes the popover as
 * it navigates — it wouldn't otherwise, since the sidebar (and this
 * popover's trigger) stays mounted across route changes.
 */
export function SettingsPopover() {
  const [isOpen, setOpen] = useState(false)
  const navigate = useNavigate()

  return (
    <Popover isOpen={isOpen} onOpenChange={setOpen}>
      <Popover.Trigger aria-label="Settings">
        <PopoverRow>
          <Settings size={16} />
          Settings
        </PopoverRow>
      </Popover.Trigger>
      <Popover.Content placement="top" className="min-w-56">
        <Popover.Dialog className="flex flex-col gap-0.5 p-2">
          <Popover.Heading className="my-2 px-4">Settings</Popover.Heading>
          <ThemeToggle />
          {SETTINGS_ITEMS.map((item) => {
            if (!item.to) return null
            const to = item.to
            return (
              <PopoverRow
                key={to}
                onPress={() => {
                  setOpen(false)
                  navigate(to)
                }}
              >
                <item.icon size={16} />
                {item.label}
              </PopoverRow>
            )
          })}
        </Popover.Dialog>
      </Popover.Content>
    </Popover>
  )
}
