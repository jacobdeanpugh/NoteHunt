import { useState, useEffect } from 'react'
import Card from '../components/ui/Card'
import FileTable from '../components/FileTable'
import Pagination from '../components/Pagination'
import { getIndexStatus } from '../api/client'

// Mock data - would fetch from API in Phase 5
const MOCK_FILES = [
  {
    filePath: '/Users/jacob/Notes/project.md',
    status: 'Complete',
    lastModified: new Date(Date.now() - 3600000).toISOString(),
    fileSize: 2400,
  },
  {
    filePath: '/Users/jacob/Notes/ideas.txt',
    status: 'Pending',
    lastModified: new Date(Date.now() - 7200000).toISOString(),
    fileSize: 1200,
  },
  {
    filePath: '/Users/jacob/Notes/broken.txt',
    status: 'Error',
    lastModified: new Date(Date.now() - 86400000).toISOString(),
    fileSize: 800,
  },
]

export default function IndexStatusScreen() {
  const [indexStatus, setIndexStatus] = useState(null)
  const [currentPage, setCurrentPage] = useState(1)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    fetchStatus()
    const interval = setInterval(fetchStatus, 5000)
    return () => clearInterval(interval)
  }, [])

  const fetchStatus = async () => {
    try {
      setLoading(true)
      const data = await getIndexStatus()
      setIndexStatus(data)
      setError(null)
    } catch (err) {
      console.error('Failed to fetch status:', err)
      setError('Failed to load index status')
    } finally {
      setLoading(false)
    }
  }

  const filesPerPage = 50
  const startIdx = (currentPage - 1) * filesPerPage
  const paginatedFiles = MOCK_FILES.slice(startIdx, startIdx + filesPerPage)
  const totalPages = Math.ceil(MOCK_FILES.length / filesPerPage)

  return (
    <div>
      <h1 className="text-3xl font-light mb-8">Index Status</h1>

      {/* Summary Cards */}
      <div className="grid grid-cols-4 gap-4 mb-8">
        <Card label="Pending">
          <div className="text-2xl font-medium text-light">
            {indexStatus?.pendingFiles || 0}
          </div>
        </Card>
        <Card label="In Progress">
          <div className="text-2xl font-medium text-light">
            {indexStatus?.inProgressFiles || 0}
          </div>
        </Card>
        <Card label="Errors">
          <div
            className={`text-2xl font-medium ${
              (indexStatus?.errorFiles || 0) > 0
                ? 'text-status-error'
                : 'text-muted'
            }`}
          >
            {indexStatus?.errorFiles || 0}
          </div>
        </Card>
        <Card label="Last Synced">
          <div className="text-sm text-body">
            {indexStatus?.lastSyncTime
              ? new Date(indexStatus.lastSyncTime).toLocaleTimeString()
              : 'Never'}
          </div>
        </Card>
      </div>

      {/* Re-index link */}
      <div className="text-right mb-4">
        <button className="text-sm text-muted hover:text-body transition-colors">
          Re-index all
        </button>
      </div>

      {/* File Table */}
      {error && (
        <div className="bg-status-error bg-opacity-10 border border-status-error text-status-error p-3 rounded mb-6">
          {error}
        </div>
      )}

      <h2 className="text-xs uppercase text-muted font-medium tracking-wide mb-4">
        File List
      </h2>
      <FileTable files={paginatedFiles} loading={loading} />

      {/* Pagination */}
      <div className="mt-6">
        <Pagination
          currentPage={currentPage}
          totalPages={totalPages}
          onPageChange={setCurrentPage}
        />
      </div>
    </div>
  )
}
