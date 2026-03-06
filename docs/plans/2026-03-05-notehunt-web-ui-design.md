# NoteHunt Web Dashboard UI Design
**Date:** March 5, 2026
**Phase:** 3 (High Priority)
**Dependencies:** Phase 1 (REST API), Phase 2 (Snippets & Ranking)

---

## Overview

NoteHunt Web Dashboard is a modern, minimal Acme-inspired web interface for searching and managing the note index. It's a React SPA served on `localhost:3000`, communicating with the Java REST API backend on `localhost:8080`.

**Design Philosophy:**
- Warm dark aesthetic (#0e0e0e background, #141414 surfaces)
- Editorial typography using weight mixing (Geist/Inter)
- No gradients, shadows, or glassmorphism — depth from typography alone
- Flat card-based layout with minimal borders (#1e1e1e)
- Icon-only sidebar navigation
- Content-dense information presentation

---

## Technology Stack

| Layer | Tech | Rationale |
|-------|------|-----------|
| **Framework** | React 18 | Modern, component-based, familiar |
| **Build Tool** | Vite | Fast build, hot reload, minimal config |
| **Styling** | Tailwind CSS + custom config | Utility-first, easy to maintain Acme palette |
| **HTTP Client** | Axios or Fetch API | Simple async data fetching |
| **State Management** | React hooks (useState, useEffect) | Lightweight, no Redux needed |
| **Routing** | React Router v6 | Client-side navigation between screens |
| **Testing** | Vitest + React Testing Library | Unit tests for components |

---

## Color Palette

```
Primary Background:    #0e0e0e (warm near-black)
Surface/Card:          #141414
Border/Divider:        #1e1e1e
Text Muted:            #555555 (labels, captions)
Text Body:             #aaaaaa
Text Light:            #f5f5f5 (buttons, high contrast)

Status Colors (no saturation):
  Complete:   muted gray (#888888)
  Pending:    muted yellow (#c9a961)
  Error:      muted red (#b85c5c)
```

---

## Typography

| Role | Font | Size | Weight | Usage |
|------|------|------|--------|-------|
| **Heading (Greeting)** | Geist | 28px | 300 | "Good morning, Jacob" |
| **Body** | Geist/Inter | 14px | 400 | Standard text, result snippets |
| **Labels** | Geist | 11px | 500 | Card titles, uppercase with letter-spacing |
| **Monospace** | Geist Mono | 12px | 400 | File paths in tables |
| **Button** | Geist | 14px | 500 | CTA text |

**Key Pattern:** Mix weights in the same line for emphasis:
`"You have **4 pending** files to index"` — light + bold

---

## Layout Architecture

### Shared Components (All Screens)

#### Sidebar (~48px wide, fixed left)
- Icon-only navigation
- NoteHunt logo mark at top
- Icons: Search, Index Status, Settings
- Active icon: faint #1e1e1e pill background (no color accent)
- Bottom: app version or user avatar (minimal)
- No text labels, maximum breathing room

#### Top Bar (full width, sticky)
- Left: Minimal "NoteHunt" text or logo
- Right: System username display, settings icon, avatar
- Height: ~56px
- Background: #0e0e0e, subtle border-bottom: 1px solid #1e1e1e

### Screen Container
- Main content area to the right of sidebar
- Padding: 32px (horizontal), 24px (vertical)
- Max-width: none (full responsive width)
- Dark background: #0e0e0e

---

## Screen 1: Search (Home)

### Purpose
Primary entry point for searching notes. Display recent/summary info, then focus on search input and results.

### Layout

```
┌──────────────────────────────────────────────────┐
│ [Logo] [Nav] Search bar [Avatar]                │ ← TopBar
├────────────┬──────────────────────────────────────┤
│ [Search]   │                                      │
│ [Index]    │  Good morning, Jacob                 │
│ [Settings] │  here's what's in your notes         │
│            │                                      │
│            │  ┌─────────┬─────────┬──────┬────┐   │
│            │  │ Recent  │ Index   │ Top  │Last│   │
│            │  │ Files   │ Status  │ Tags │Srch│   │
│            │  │ 3 files │ 1,204 f │#idea │    │   │
│            │  │ ...     │ ...     │...   │... │   │
│            │  └─────────┴─────────┴──────┴────┘   │
│            │                                      │
│            │  I want to find... notes about dllns│
│            │  I want to find... tagged #ideas    │
│            │  I want to find... files I edited...│
│            │                                      │
│            │  ┌─────────────────────────────────┐ │
│            │  │ I want to find...  [🔍] [icons] │ │
│            │  └─────────────────────────────────┘ │
│            │                                      │
│            │  SEARCH RESULTS (if any)            │
│            │  ┌─────────────────────────────────┐ │
│            │  │ /path/to/file.txt  Score: 0.95  │ │
│            │  │ Mod: 2h ago • 2.4KB • #work     │ │
│            │  │ ...snippet context...           │ │
│            │  ├─────────────────────────────────┤ │
│            │  │ /other/file.md     Score: 0.87  │ │
│            │  │ Mod: 3d ago • 1.8KB • #ideas    │ │
│            │  │ ...snippet context...           │ │
│            │  └─────────────────────────────────┘ │
│            │                                      │
│            │  [← Prev] Page 1 of 3 [Next →]      │
└────────────┴──────────────────────────────────────┘
```

### Components

#### Summary Cards (4-column grid)
Each card:
- Label (11px, muted caps): "Recent Files", "Index Status", "Top Tags", "Last Search"
- Main content: large, mixed-weight text
- Bottom: "See more →" link (muted, text link not button)
- Style: #141414 bg, 1px #1e1e1e border, 6px radius, no shadow

**Card Content:**
1. **Recent Files** — "3 files modified today" + list of recent files
2. **Index Status** — "1,204 files indexed" + status breakdown (Pending/Complete/Error)
3. **Top Tags** — "#ideas · #work · #research" + tag cloud
4. **Last Search** — "meeting notes" + time ago

#### Search Input Section
- Heading: "I want to find..." in 14px body text
- 3 prompt starters (click to populate input):
  - "...notes about project deadlines"
  - "...everything tagged #ideas"
  - "...files I edited last week"
- Input field:
  - Full-width
  - Placeholder: "I want to find..."
  - Background: #141414
  - Border: 1px solid #1e1e1e
  - Radius: 4px
  - Icon toolbar bottom-left: [attach] [search mode]
  - Send button bottom-right: "→" (arrow, no label)
  - On submit: Call `/search?q=<query>&limit=20&offset=0`

#### Results Grid
- Cards, each showing:
  - **File Path** (monospace, 12px, #aaaaaa)
  - **Relevance Score** (top-right, "Score: 0.95")
  - **Metadata**: last modified time (relative, e.g., "2h ago"), file size (KB/MB), tags (#work, #ideas)
  - **Snippet** (14px body text, context around matched terms, 150-200 chars)
  - Card style: #141414 bg, 1px #1e1e1e border, 6px radius

#### Pagination
- Simple centered controls: "[← Prev] Page X of Y [Next →]"
- Text links, no buttons
- Muted color (#555555)

---

## Screen 2: Index Status

### Purpose
Monitor indexing progress, see file list, manually trigger re-indexing.

### Layout

```
┌──────────────────────────────────────────────────┐
│ [Logo] [Nav]                          [Avatar]  │ ← TopBar
├────────────┬──────────────────────────────────────┤
│ [Search]   │                                      │
│ [Index]    │  INDEX STATUS                        │
│ [Settings] │                                      │
│            │  ┌──────────┬──────┬────────┬──────┐ │
│            │  │ Pending  │ Prog │ Errors │Syncd│ │
│            │  │ 12       │ 3    │ 0      │ 2m  │ │
│            │  └──────────┴──────┴────────┴──────┘ │
│            │                           [Re-index] │
│            │                                      │
│            │  FILE TABLE                          │
│            │  ──────────────────────────────────  │
│            │  /path/to/file.txt        Complete   │
│            │  /another/file.md         Pending    │
│            │  /broken/file.txt         Error      │
│            │  ...                                 │
│            │  ──────────────────────────────────  │
│            │                                      │
│            │  [← Prev] Page 1 of 5 [Next →]      │
└────────────┴──────────────────────────────────────┘
```

### Components

#### Summary Cards (4 columns)
Same style as Screen 1:
- **Pending:** Count of files waiting to be indexed
- **In Progress:** Count currently being indexed
- **Errors:** Red text if > 0; muted if 0
- **Last Synced:** Timestamp, e.g., "2 minutes ago"

#### File Table
- **Columns:** File Path | Last Modified | Status | Size
- **Path:** monospace, 12px, #aaaaaa
- **Modified:** relative time (e.g., "3h ago")
- **Status:** plain text, colored:
  - Complete: #888888 (muted)
  - Pending: #c9a961 (muted yellow)
  - Error: #b85c5c (muted red)
- **Size:** 1.2KB format
- **Row dividers:** 1px solid #1e1e1e (no zebra striping)
- **Pagination:** 50 files per page

#### Re-index Button
- Text link: "Re-index all" (top-right corner)
- Muted color, not a button
- On click: POST to backend endpoint (future: Phase 5)

---

## Screen 3: Settings

### Purpose
Configure NoteHunt behavior (directory, file types, batch size).

### Layout

```
┌──────────────────────────────────────────────────┐
│ [Logo] [Nav]                          [Avatar]  │ ← TopBar
├────────────┬──────────────────────────────────────┤
│ [Search]   │                                      │
│ [Index]    │  SETTINGS                            │
│ [Settings] │                                      │
│            │  DIRECTORY SETTINGS                  │
│            │  ───────────────────────────────────  │
│            │  Directory Path                      │
│            │  [/Users/jacob/Notes_______]        │
│            │                                      │
│            │  Index Path                          │
│            │  [%APPDATA%/NoteQuest/index]        │
│            │                                      │
│            │  FILE EXTENSIONS                     │
│            │  ───────────────────────────────────  │
│            │  ☑ .txt  ☐ .md  ☐ .rst  ☐ .org     │
│            │                                      │
│            │  INDEXING                            │
│            │  ───────────────────────────────────  │
│            │  Batch Size (files per batch)        │
│            │  [50_________]                      │
│            │                                      │
│            │                        [Save Settings]│
└────────────┴──────────────────────────────────────┘
```

### Components

#### Section Headers
- 11px uppercase, #555555 color
- 0.08em letter-spacing
- Bottom border: 1px solid #1e1e1e
- Padding: 8px 0

#### Input Fields
- Background: #141414
- Border: 1px solid #1e1e1e
- Radius: 4px
- Padding: 8px 12px
- Font: 14px, #aaaaaa
- Placeholder: muted (#555555)

#### Checkbox/Toggle
- Simple monospace checkbox: ☑ .txt
- No styled toggle switches
- On/off: checked/unchecked state only

#### Save Button
- Label: "Save Settings"
- Style: solid button (ONLY light element on page)
  - Background: #f5f5f5
  - Text: #0e0e0e
  - Padding: 10px 20px
  - Radius: 4px
- Bottom-right corner
- On click: POST config changes to backend (future endpoint)

**Note:** Currently read-only. Write functionality added in Phase 5.

---

## API Integration

### Endpoints Used

| Endpoint | Method | Purpose | Screen |
|----------|--------|---------|--------|
| `/search?q=<query>&limit=20&offset=0` | GET | Fetch search results | Screen 1 |
| `/index/status` | GET | Index statistics | Screens 1 & 2 |
| `/config` | GET | Fetch current config | Screen 3 |
| `/config` | POST | Save config changes | Screen 3 (Phase 5) |
| `/tags` | GET | List available tags | Screen 1 (Phase 4) |

### Response Format (Expected from Backend)

**`GET /search`** (Phase 1.3 + 2.1 + 2.2)
```json
{
  "query": "meeting notes",
  "totalResults": 42,
  "page": 1,
  "pageSize": 20,
  "results": [
    {
      "filePath": "/path/to/file.txt",
      "relevanceScore": 0.95,
      "snippet": "...matched text context...",
      "tags": ["work", "project"],
      "lastModified": "2026-03-05T10:30:00Z",
      "fileSize": 2400
    }
  ]
}
```

**`GET /index/status`** (Phase 1.1)
```json
{
  "totalFiles": 1204,
  "completedFiles": 1180,
  "pendingFiles": 12,
  "inProgressFiles": 3,
  "errorFiles": 9,
  "lastSyncTime": "2026-03-05T10:35:00Z"
}
```

---

## Component Hierarchy

```
App.jsx (routing)
├─ Layout.jsx (shared sidebar + topbar)
│  ├─ Sidebar.jsx
│  └─ TopBar.jsx
├─ SearchScreen.jsx
│  ├─ SummaryCards.jsx
│  │  ├─ Card.jsx
│  │  ├─ Card.jsx
│  │  ├─ Card.jsx
│  │  └─ Card.jsx
│  ├─ SearchInput.jsx
│  ├─ ResultsList.jsx
│  │  ├─ ResultCard.jsx
│  │  ├─ ResultCard.jsx
│  │  └─ ResultCard.jsx
│  └─ Pagination.jsx
├─ IndexStatusScreen.jsx
│  ├─ SummaryCards.jsx
│  ├─ FileTable.jsx
│  │  ├─ TableRow.jsx
│  │  └─ TableRow.jsx
│  └─ Pagination.jsx
└─ SettingsScreen.jsx
   ├─ FormSection.jsx
   ├─ InputField.jsx
   ├─ CheckboxGroup.jsx
   └─ SaveButton.jsx
```

---

## Styling Approach

### Tailwind CSS Configuration
Custom config in `tailwind.config.js`:
```javascript
module.exports = {
  theme: {
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
      sans: ['Geist', 'Inter', 'system-ui'],
      mono: ['Geist Mono', 'monospace'],
    },
  },
}
```

### Design Rules
- **No shadows:** Use only 1px solid borders for depth
- **Max border-radius:** 6px (cards), 4px (inputs/buttons)
- **No gradients:** Flat colors only
- **No glassmorphism:** No blur, backdrop-filter, or transparency overlays
- **Spacing:** Multiples of 4px (4, 8, 12, 16, 20, 24, 32)

---

## Accessibility & UX

### Keyboard Navigation
- Tab through inputs, buttons, links
- Enter to submit forms, search
- Escape to close modals (future)

### Color Contrast
- Body text (#aaaaaa) on #141414 background: 5.5:1 ratio ✓
- Muted text (#555555) on #141414: 3.2:1 ratio (acceptable for labels)
- Light text (#f5f5f5) on #0e0e0e: 16:1+ ratio ✓

### Status Indication
- Rely on text labels, not color alone
- Example: "Error (5 files)" not just red number

### Responsive Design
- Sidebar collapses on mobile (future: Phase 5)
- Cards stack vertically on small screens
- Results grid: 2 columns on tablet, 1 on mobile

---

## Future Enhancements (Post-Phase 3)

1. **Phase 4 Integration:** Tag filters in sidebar, tag auto-complete in search
2. **Phase 5 Integration:** Settings save functionality, re-index endpoint
3. **Error States:** Toast notifications for failed API calls
4. **Loading States:** Skeleton loaders for results, stats
5. **Mobile:** Responsive Sidebar collapse, touch-friendly inputs
6. **Dark/Light Theme Toggle:** (optional, Acme design is dark-first)
7. **Saved Searches:** Bookmark favorite queries in sidebar
8. **Advanced Search:** UI toggle between simple and advanced query syntax

---

## Success Criteria

✅ **Visual Fidelity**
- Matches Acme aesthetic exactly (warm dark, editorial, no effects)
- Typography weight mixing works for emphasis
- All cards and inputs use specified colors

✅ **Functional**
- Search results display with full data (score, snippet, tags, size, date)
- Index status updates every 5 seconds (or on-demand)
- Pagination works on all paginated views
- Navigation between screens works via sidebar

✅ **Performance**
- Initial load < 2 seconds (empty cache)
- Search results display < 500ms after API response
- Smooth scrolling on results list

✅ **Testing**
- Unit tests for 5+ critical components
- Integration tests for search flow
- No console errors or warnings

---

**Design Approved By:** [User]
**Design Review Date:** March 5, 2026
**Next Step:** Invoke writing-plans skill to create detailed implementation plan
