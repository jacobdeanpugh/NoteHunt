import { describe, it, expect, vi, beforeEach } from 'vitest'
import { searchNotes, getIndexStatus } from '../client'

global.fetch = vi.fn()

describe('API Client', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('searchNotes should call /search endpoint with query params', async () => {
    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ results: [] })
    })

    const result = await searchNotes('test query', 1, 20)

    expect(global.fetch).toHaveBeenCalled()
    const [url] = global.fetch.mock.calls[0]
    expect(url).toContain('/search')
    expect(url).toContain('q=test+query')
    expect(url).toContain('offset=0')
    expect(url).toContain('limit=20')
    expect(result).toEqual({ results: [] })
  })

  it('getIndexStatus should call /index/status endpoint', async () => {
    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ totalFiles: 1204 })
    })

    const result = await getIndexStatus()

    expect(global.fetch).toHaveBeenCalled()
    const [url] = global.fetch.mock.calls[0]
    expect(url).toContain('/index/status')
    expect(result.totalFiles).toBe(1204)
  })

  it('should throw error on failed response', async () => {
    global.fetch.mockResolvedValueOnce({
      ok: false,
      status: 500
    })

    await expect(searchNotes('test')).rejects.toThrow('HTTP error')
  })
})
