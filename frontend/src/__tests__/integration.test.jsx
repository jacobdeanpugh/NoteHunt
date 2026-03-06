import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import SearchScreen from '../screens/SearchScreen'

vi.mock('../api/client', () => ({
  searchNotes: vi.fn(async () => ({
    results: [
      {
        filePath: '/test/file.txt',
        score: 0.95,
        snippet: 'test snippet',
        tags: ['test'],
        lastModified: new Date().toISOString(),
        fileSize: 1024,
      },
    ],
    totalResults: 1,
    page: 1,
    pageSize: 20,
  })),
  getIndexStatus: vi.fn(async () => ({
    totalFiles: 100,
    completedFiles: 95,
    pendingFiles: 5,
    inProgressFiles: 0,
    errorFiles: 0,
    lastSyncTime: new Date().toISOString(),
  })),
}))

describe('Search Integration', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('displays summary cards on mount', async () => {
    render(
      <BrowserRouter>
        <SearchScreen />
      </BrowserRouter>
    )

    await waitFor(() => {
      // Check for summary card titles
      expect(screen.getByText('Recent Files')).toBeInTheDocument()
      expect(screen.getByText('Index Status')).toBeInTheDocument()
    })
  })

  it('renders prompt starters in SearchInput', async () => {
    render(
      <BrowserRouter>
        <SearchScreen />
      </BrowserRouter>
    )

    // Check for prompt starters
    await waitFor(() => {
      expect(screen.getByText(/notes about project deadlines/i)).toBeInTheDocument()
      expect(screen.getByText(/everything tagged #ideas/i)).toBeInTheDocument()
    })
  })
})
