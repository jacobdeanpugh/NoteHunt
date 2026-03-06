export default {
  content: [
    "./index.html",
    "./src/**/*.{js,jsx,ts,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        bg: {
          primary: '#0e0e0e',
          surface: '#141414',
        },
        border: {
          default: '#1e1e1e',
        },
        text: {
          muted: '#555555',
          body: '#aaaaaa',
          light: '#f5f5f5',
        },
        status: {
          complete: '#888888',
          pending: '#c9a961',
          error: '#b85c5c',
        },
      },
      fontFamily: {
        sans: ['Geist', 'Inter', 'ui-sans-serif', 'system-ui'],
        mono: ['Geist Mono', 'monospace'],
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
    },
  },
  plugins: [],
}
