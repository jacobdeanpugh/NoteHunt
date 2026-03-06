# NoteHunt Web Dashboard Build

## Development

```bash
npm install
npm run dev
```

Server runs at http://localhost:3000

## Production Build

```bash
npm run build
```

Output: `dist/` directory

## Testing

```bash
npm test
npm run test:ui
```

## API Proxy

Development server proxies `/api/*` to `http://localhost:8080`

## Environment Variables

Copy `.env.example` to `.env.local` and customize:
- `VITE_API_URL` - Backend API URL
