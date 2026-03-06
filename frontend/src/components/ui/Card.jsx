export default function Card({ label, children, cta = null, className = '' }) {
  return (
    <div
      data-testid="card"
      className={`bg-bg-surface border border-border-default rounded-md p-6 ${className}`}
    >
      {label && (
        <div className="flex justify-between items-start mb-4">
          <h3 className="text-xs uppercase text-text-muted font-medium tracking-wide">
            {label}
          </h3>
          {cta && (
            <a href="#" className="text-xs text-text-muted hover:text-text-body">
              {cta} →
            </a>
          )}
        </div>
      )}
      <div>{children}</div>
    </div>
  )
}
