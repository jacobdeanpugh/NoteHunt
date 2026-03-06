export default function Button({
  children,
  onClick,
  variant = 'primary',
  className = '',
  disabled = false,
}) {
  const variants = {
    primary: 'bg-text-[#f5f5f5] text-bg-[#0e0e0e] hover:opacity-90',
    secondary: 'bg-[#141414] border border-[#1e1e1e] text-[#aaaaaa] hover:bg-[#141414]',
    text: 'text-[#555555] hover:text-[#aaaaaa]',
  }

  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className={`px-4 py-2 rounded transition-colors font-medium text-sm ${variants[variant]} ${className} ${
        disabled ? 'opacity-50 cursor-not-allowed' : ''
      }`}
    >
      {children}
    </button>
  )
}
