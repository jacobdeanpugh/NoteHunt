import Card from './ui/Card'

export default function SummaryCards({
  recentFiles = 0,
  indexStatus = {},
  topTags = [],
  lastSearch = '',
  loading = true,
}) {
  if (loading) {
    return (
      <div className="grid grid-cols-4 gap-4 mb-8">
        {[...Array(4)].map((_, i) => (
          <div
            key={i}
            className="bg-bg-surface border border-border-default rounded p-6 h-32 animate-pulse"
          />
        ))}
      </div>
    )
  }

  const { total = 0, complete = 0, pending = 0, error = 0 } = indexStatus

  return (
    <div className="grid grid-cols-4 gap-4 mb-8">
      {/* Recent Files */}
      <Card label="Recent Files" cta="See more">
        <div className="font-light text-lg">
          <span className="font-medium">{recentFiles}</span> files modified today
        </div>
      </Card>

      {/* Index Status */}
      <Card label="Index Status" cta="See more">
        <div className="space-y-2">
          <div className="font-light text-lg">
            <span className="font-medium">{total.toLocaleString()}</span> files indexed
          </div>
          <div className="text-xs text-text-muted">
            {complete} complete · {pending} pending · {error} errors
          </div>
        </div>
      </Card>

      {/* Top Tags */}
      <Card label="Top Tags" cta="See more">
        <div className="text-sm text-text-body">
          {topTags.length > 0
            ? topTags.slice(0, 3).join(' · ')
            : 'No tags yet'}
        </div>
      </Card>

      {/* Last Search */}
      <Card label="Last Search" cta="See more">
        <div className="font-light text-lg">
          {lastSearch || 'No recent searches'}
        </div>
      </Card>
    </div>
  )
}
