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
        aria-label={placeholder}
        className={`w-full bg-[#141414] border border-[#1e1e1e] rounded px-3 py-2 text-[#aaaaaa] placeholder-text-[#555555] focus:outline-none focus:border-text-[#aaaaaa] transition-colors ${className}`}
      />
      {icon && (
        <div className="absolute right-3 top-1/2 transform -translate-y-1/2 text-[#555555]">
          {icon}
        </div>
      )}
    </div>
  )
}
