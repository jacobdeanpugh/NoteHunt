import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import Card from '../Card'

describe('Card Component', () => {
  it('renders with label and children', () => {
    render(
      <Card label="Test Label">
        <div>Card content</div>
      </Card>
    )

    expect(screen.getByText('Test Label')).toBeInTheDocument()
    expect(screen.getByText('Card content')).toBeInTheDocument()
  })

  it('applies correct styling classes', () => {
    const { container } = render(
      <Card label="Test">
        <div>Content</div>
      </Card>
    )

    const cardElement = container.querySelector('[data-testid="card"]')
    expect(cardElement).toHaveClass('bg-[#141414]', 'border-[#1e1e1e]')
  })
})
