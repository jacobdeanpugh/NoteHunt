export default function FileTable({ files = [], loading = false }) {
  const getStatusColor = (status) => {
    switch (status?.toLowerCase()) {
      case 'complete':
      case 'pending':
        return 'text-status-pending'
      case 'in_progress':
        return 'text-text-muted'
      case 'error':
        return 'text-status-error'
      default:
        return 'text-text-body'
    }
  }

  const formatTime = (iso) => {
    const date = new Date(iso)
    const now = new Date()
    const diffMs = now - date
    const diffMins = Math.floor(diffMs / 60000)
    const diffHours = Math.floor(diffMs / 3600000)
    const diffDays = Math.floor(diffMs / 86400000)

    if (diffMins < 1) return 'just now'
    if (diffMins < 60) return `${diffMins}m ago`
    if (diffHours < 24) return `${diffHours}h ago`
    return `${diffDays}d ago`
  }

  const formatSize = (bytes) => {
    if (bytes < 1024) return `${bytes}B`
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)}KB`
    return `${(bytes / (1024 * 1024)).toFixed(1)}MB`
  }

  if (loading) {
    return (
      <div className="space-y-2">
        {[...Array(10)].map((_, i) => (
          <div
            key={i}
            className="bg-bg-surface border-b border-border-default h-8 animate-pulse"
          />
        ))}
      </div>
    )
  }

  return (
    <div className="border border-border-default rounded overflow-hidden">
      {/* Header */}
      <div className="grid grid-cols-3 gap-4 bg-border-default px-4 py-2 text-xs uppercase text-text-muted font-medium">
        <div>File Path</div>
        <div>Modified</div>
        <div className="flex justify-between">
          <span>Status</span>
          <span>Size</span>
        </div>
      </div>

      {/* Rows */}
      <div>
        {files.length === 0 ? (
          <div className="p-4 text-center text-text-muted text-sm">
            No files to display
          </div>
        ) : (
          files.map((file, idx) => (
            <div
              key={idx}
              className="grid grid-cols-3 gap-4 px-4 py-3 border-t border-border-default text-sm hover:bg-bg-surface transition-colors"
            >
              <div className="font-mono text-text-body truncate">
                {file.filePath}
              </div>
              <div className="text-text-muted">
                {formatTime(file.lastModified)}
              </div>
              <div className="flex justify-between items-center">
                <span className={`${getStatusColor(file.status)}`}>
                  {file.status}
                </span>
                <span className="text-text-muted">{formatSize(file.fileSize)}</span>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  )
}
