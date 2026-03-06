import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import SummaryCards from './SummaryCards'

describe('SummaryCards', () => {
  const mockData = {
    recentFiles: 3,
    indexStatus: { total: 1204, complete: 1180, pending: 12, error: 9 },
    topTags: ['#ideas', '#work', '#research'],
    lastSearch: 'meeting notes',
  }

  it('renders 4 summary cards', () => {
    render(<SummaryCards {...mockData} loading={false} />)

    expect(screen.getByText('Recent Files')).toBeInTheDocument()
    expect(screen.getByText('Index Status')).toBeInTheDocument()
    expect(screen.getByText('Top Tags')).toBeInTheDocument()
    expect(screen.getByText('Last Search')).toBeInTheDocument()
  })

  it('displays recent files count', () => {
    render(<SummaryCards {...mockData} loading={false} />)
    expect(screen.getByText('3')).toBeInTheDocument()
    expect(screen.getByText(/files modified today/)).toBeInTheDocument()
  })

  it('displays index statistics', () => {
    render(<SummaryCards {...mockData} loading={false} />)
    expect(screen.getByText('1,204')).toBeInTheDocument()
    expect(screen.getByText(/files indexed/)).toBeInTheDocument()
  })
})
