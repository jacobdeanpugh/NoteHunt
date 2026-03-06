export default function Button({
  children,
  onClick,
  variant = 'primary',
  className = '',
  disabled = false,
}) {
  const variants = {
    primary: 'bg-text-light text-bg-primary hover:opacity-90',
    secondary: 'bg-surface border border-dark text-body hover:bg-surface',
    text: 'text-muted hover:text-body',
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
