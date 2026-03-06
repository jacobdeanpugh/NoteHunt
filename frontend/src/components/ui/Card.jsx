export default function Card({ label, children, cta = null, className = '' }) {
  return (
    <div
      data-testid="card"
      className={`bg-[#141414] border border-[#1e1e1e] rounded-md p-6 ${className}`}
    >
      {label && (
        <div className="flex justify-between items-start mb-4">
          <h3 className="text-xs uppercase text-[#555555] font-medium tracking-wide">
            {label}
          </h3>
          {cta && (
            <a href="#" className="text-xs text-[#555555] hover:text-[#aaaaaa]">
              {cta} →
            </a>
          )}
        </div>
      )}
      <div>{children}</div>
    </div>
  )
}
