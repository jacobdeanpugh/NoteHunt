export default {
  content: [
    "./index.html",
    "./src/**/*.{js,jsx,ts,tsx}",
  ],
  theme: {
    colors: {
      // Acme dark theme
      'primary': '#0e0e0e',
      'surface': '#141414',
      'border-dark': '#1e1e1e',
      'body': '#aaaaaa',
      'muted': '#555555',
      'light': '#f5f5f5',
      'complete': '#888888',
      'pending': '#c9a961',
      'error': '#b85c5c',
      // Keep some basic colors for standard Tailwind usage
      transparent: 'transparent',
      current: 'currentColor',
    },
    fontFamily: {
      sans: ['Inter', 'system-ui', '-apple-system', 'sans-serif'],
      mono: ['monospace'],
    },
    fontSize: {
      xs: ['11px', { lineHeight: '1.3' }],
      sm: ['12px', { lineHeight: '1.4' }],
      base: ['14px', { lineHeight: '1.5' }],
      lg: ['28px', { lineHeight: '1.2' }],
    },
    letterSpacing: {
      wide: '0.08em',
    },
    extend: {},
  },
  plugins: [],
}
