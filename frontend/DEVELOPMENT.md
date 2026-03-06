# Development Guide

## Setup

1. Install dependencies: `npm install`
2. Ensure Java backend runs: `http://localhost:8080`
3. Start dev server: `npm run dev`

## Adding a New Component

1. Create component file: `src/components/MyComponent.jsx`
2. Write test file: `src/components/__tests__/MyComponent.test.jsx`
3. Export from component index (if using one)
4. Use in screens

## Testing

- Run all: `npm test`
- Watch mode: `npm test -- --watch`
- UI dashboard: `npm run test:ui`

## Styling Guidelines

- Use Tailwind utilities
- Custom colors in `tailwind.config.js`
- Max border-radius: 6px
- No shadows (use borders)
- No gradients

## API Integration

- Client at `src/api/client.js`
- Backend expected at `http://localhost:8080`
- Dev server proxies `/api/*`

## Component Hierarchy

```
App (routing)
└── Layout
    ├── Sidebar
    ├── TopBar
    └── Screen (SearchScreen | IndexStatusScreen | SettingsScreen)
```

## Color Reference

- Primary BG: `#0e0e0e`
- Surface: `#141414`
- Border: `#1e1e1e`
- Text Body: `#aaaaaa`
- Text Muted: `#555555`
- Text Light: `#f5f5f5`
- Status Complete: `#888888`
- Status Pending: `#c9a961`
- Status Error: `#b85c5c`
