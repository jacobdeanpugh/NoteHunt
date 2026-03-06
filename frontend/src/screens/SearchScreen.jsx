import { useState, useEffect } from 'react'
import SummaryCards from '../components/SummaryCards'
import SearchInput from '../components/SearchInput'
import SearchResults from '../components/SearchResults'
import { searchNotes, getIndexStatus } from '../api/client'

export default function SearchScreen() {
  const [query, setQuery] = useState('')
  const [currentPage, setCurrentPage] = useState(1)
  const [searchResults, setSearchResults] = useState(null)
  const [indexStatus, setIndexStatus] = useState(null)
  const [loading, setLoading] = useState(false)
  const [statusLoading, setStatusLoading] = useState(true)
  const [error, setError] = useState(null)

  // Fetch index status on mount
  useEffect(() => {
    fetchStatus()
    const interval = setInterval(fetchStatus, 5000) // Refresh every 5 seconds
    return () => clearInterval(interval)
  }, [])

  // Fetch search results when query or page changes
  useEffect(() => {
    if (query) {
      performSearch()
    }
  }, [query, currentPage])

  const fetchStatus = async () => {
    try {
      setStatusLoading(true)
      const data = await getIndexStatus()
      setIndexStatus(data)
      setError(null)
    } catch (err) {
      console.error('Failed to fetch status:', err)
      setError('Failed to load index status')
    } finally {
      setStatusLoading(false)
    }
  }

  const performSearch = async () => {
    try {
      setLoading(true)
      setError(null)
      const results = await searchNotes(query, currentPage, 20)
      setSearchResults(results)
    } catch (err) {
      console.error('Search failed:', err)
      setError('Search failed. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  const handleSearch = (searchQuery) => {
    setQuery(searchQuery)
    setCurrentPage(1)
  }

  const handlePageChange = (newPage) => {
    setCurrentPage(newPage)
    window.scrollTo(0, 0)
  }

  // Get system username
  const username = (import.meta.env.VITE_USER || 'User')
    .charAt(0)
    .toUpperCase() + (import.meta.env.VITE_USER || 'User').slice(1)

  return (
    <div>
      {/* Greeting */}
      <div className="mb-12">
        <h1 className="text-5xl font-light mb-2">Good morning, {username}</h1>
        <p className="text-[#555555]">here's what's in your notes</p>
      </div>

      {/* Summary Cards */}
      <SummaryCards
        recentFiles={indexStatus?.completedFiles || 0}
        indexStatus={indexStatus || {}}
        topTags={[]}
        lastSearch={query}
        loading={statusLoading}
      />

      {/* Search Input */}
      <SearchInput onSearch={handleSearch} loading={loading} />

      {/* Error message */}
      {error && (
        <div className="bg-status-error bg-opacity-10 border border-status-error text-status-error p-3 rounded mb-6 flex justify-between items-center">
          <span>{error}</span>
          <button
            onClick={() => {
              if (query) performSearch()
              else fetchStatus()
            }}
            className="text-sm underline hover:no-underline"
          >
            Retry
          </button>
        </div>
      )}

      {/* Search Results */}
      {query && (
        <SearchResults
          results={searchResults?.results || []}
          totalResults={searchResults?.totalResults || 0}
          currentPage={currentPage}
          pageSize={20}
          loading={loading}
          onPageChange={handlePageChange}
        />
      )}
    </div>
  )
}
