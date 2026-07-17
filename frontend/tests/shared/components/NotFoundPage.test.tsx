import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { NotFoundPage } from '@/shared/components/NotFoundPage'

describe('NotFoundPage', () => {
  it('shows a 404 message and a link home', () => {
    render(
      <MemoryRouter>
        <NotFoundPage />
      </MemoryRouter>,
    )

    expect(screen.getByText('404')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /trang chủ/i })).toHaveAttribute('href', '/')
  })
})
