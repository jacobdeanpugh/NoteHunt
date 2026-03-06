export default function ResultCard({
  filePath,
  score,
  snippet,
  tags = [],
  lastModified,
  fileSize,
}) {
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

  return (
    <div className="bg-bg-surface border border-border-default rounded p-6 mb-4">
      {/* Header: Path + Score */}
      <div className="flex justify-between items-start mb-2">
        <p className="font-mono text-sm text-text-body break-all">{filePath}</p>
        <span className="text-xs text-text-muted whitespace-nowrap ml-4">
          Score: {(score * 100).toFixed(0)}%
        </span>
      </div>

      {/* Metadata: Modified, Size, Tags */}
      <div className="text-xs text-text-muted mb-3 flex gap-4 flex-wrap">
        <span>Modified: {formatTime(lastModified)}</span>
        <span>•</span>
        <span>{formatSize(fileSize)}</span>
        {tags.length > 0 && (
          <>
            <span>•</span>
            <span>{tags.map((t) => `#${t}`).join(' ')}</span>
          </>
        )}
      </div>

      {/* Snippet */}
      <p className="text-sm text-text-body line-clamp-2 italic">
        ...{snippet}...
      </p>
    </div>
  )
}
