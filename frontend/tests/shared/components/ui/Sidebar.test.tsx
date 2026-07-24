import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { Sidebar, SidebarProvider, SidebarTrigger } from '@/shared/components/ui/Sidebar'

function renderSidebar() {
  return render(
    <SidebarProvider>
      <Sidebar>
        <div>menu content</div>
      </Sidebar>
      <SidebarTrigger />
    </SidebarProvider>,
  )
}

describe('Sidebar', () => {
  it('is open by default and shows its content', () => {
    renderSidebar()
    expect(screen.getByText('menu content')).toBeVisible()
    expect(screen.getByRole('button', { name: /close sidebar/i })).toBeInTheDocument()
  })

  it('hides content when the trigger closes it, and restores on reopen', async () => {
    const user = userEvent.setup()
    renderSidebar()

    await user.click(screen.getByRole('button', { name: /close sidebar/i }))
    expect(screen.getByText('menu content')).not.toBeVisible()

    await user.click(screen.getByRole('button', { name: /open sidebar/i }))
    expect(screen.getByText('menu content')).toBeVisible()
  })
})
