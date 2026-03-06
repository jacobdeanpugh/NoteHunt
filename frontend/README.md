# NoteHunt Web Dashboard

Modern, minimal React SPA for searching notes. Built with Acme-inspired dark design.

## Quick Start

```bash
cd frontend
npm install
npm run dev
```

Server runs at **http://localhost:3000**

Requires NoteHunt Java backend running on **http://localhost:8080**

## Features

- **Search** - Full-text search with results ranking, pagination
- **Index Status** - Real-time indexing progress monitoring
- **Settings** - Configuration management (read-only, Phase 5)

## Design

- Warm dark aesthetic (#0e0e0e background)
- Editorial typography (Geist/Inter fonts)
- No gradients, shadows, or glassmorphism
- Icon-only sidebar navigation

## Tech Stack

- React 18
- Vite (build)
- Tailwind CSS
- React Router
- Vitest + React Testing Library

## Available Scripts

- `npm run dev` - Start dev server
- `npm run build` - Production build
- `npm test` - Run tests
- `npm run test:ui` - Test UI dashboard

## Architecture

```
src/
├── api/           # API client
├── components/    # Reusable UI components
│  └── ui/        # Primitive components
├── screens/       # Full-page screens
└── App.jsx        # Root component with routing
```

## Future (Phase 4-5)

- Tag filtering and auto-complete
- Settings persistence
- Advanced search syntax UI
- Mobile responsiveness
- Dark/light theme toggle

## Contributing

See [DEVELOPMENT.md](./DEVELOPMENT.md)
