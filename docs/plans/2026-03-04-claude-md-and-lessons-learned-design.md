# Design: CLAUDE.md + LESSONS_LEARNED.md

**Date:** 2026-03-04

## Goal
Give general Claude subagents fast, accurate project context without context rot. Enable them to automatically record issue/solution pairs so future agents benefit from prior work.

## Files to Create

### CLAUDE.md (root)
- **Purpose:** Single-source-of-truth context file auto-loaded by Claude Code
- **Principle:** Concise. No duplication of ARCHITECTURE.md or ROADMAP.md — link instead.
- **Sections:**
  1. Project Identity (2-3 lines)
  2. Build & Test Commands (exact commands)
  3. Directory Structure (key paths only)
  4. Architecture & Key Classes (one-line per class)
  5. Database Schema (table + columns)
  6. Configuration (fields + purpose)
  7. Current Status (done / next)
  8. Known Pitfalls (pre-seeded, agent-maintained)
  9. Subagent Instructions (how to read + write LESSONS_LEARNED.md)

### LESSONS_LEARNED.md (root)
- **Purpose:** Accumulated issue/solution pairs written by subagents automatically
- **Trigger:** Agent hits an issue and finds a fix → appends entry
- **Format:**
  ```
  ### [Short title]
  **Issue:** What went wrong
  **Solution:** What fixed it
  ```
- **Organization:** Sections by component (Testing, Database, FileIndexer, Build/Maven, etc.)
- **Pre-seeded** with known issues from existing project knowledge

## Subagent Behavior (encoded in CLAUDE.md)
1. Read LESSONS_LEARNED.md before starting work on a component
2. On issue resolved: append entry under matching section (create section if needed)
3. Do not duplicate existing entries — scan before writing

## Non-Goals
- No manual curation required from the user
- No session-specific or temporary notes in either file
- CLAUDE.md does not replace ARCHITECTURE.md or ROADMAP.md
