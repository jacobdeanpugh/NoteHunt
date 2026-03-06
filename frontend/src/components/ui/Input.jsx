export default function Input({
  placeholder = '',
  value = '',
  onChange,
  type = 'text',
  className = '',
  icon = null,
}) {
  return (
    <div className="relative">
      <input
        type={type}
        placeholder={placeholder}
        value={value}
        onChange={onChange}
        className={`w-full bg-bg-surface border border-border-default rounded px-3 py-2 text-text-body placeholder-text-muted focus:outline-none focus:border-text-body transition-colors ${className}`}
      />
      {icon && (
        <div className="absolute right-3 top-1/2 transform -translate-y-1/2 text-text-muted">
          {icon}
        </div>
      )}
    </div>
  )
}
