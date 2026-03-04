# CLAUDE.md + LESSONS_LEARNED.md Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Create a concise CLAUDE.md for subagent context and a pre-seeded LESSONS_LEARNED.md that subagents auto-update with issue/solution pairs.

**Architecture:** Two files at the repo root. CLAUDE.md is the fast-orientation doc; it tells subagents to read LESSONS_LEARNED.md before touching any component and to append entries when they resolve issues.

**Tech Stack:** Markdown only — no code changes.

---

### Task 1: Create CLAUDE.md

**Files:**
- Create: `CLAUDE.md`

**Step 1: Write the file**

```markdown
# NoteHunt — Agent Context

NoteHunt is a Java full-text search service that monitors a directory, indexes `.txt` files via Apache Lucene, and persists file state in H2. The search API does not exist yet.

## Build & Test
```bash
cd notehunt
mvn clean package          # build shaded JAR
mvn test                   # run tests (46/52 pass; 6 disabled in FileIndexerTest)
java -cp target/notehunt-1.0-SNAPSHOT.jar dev.notequest.App  # run
```

## Key Paths
```
notehunt/src/main/java/dev/notequest/   # source
notehunt/src/test/java/dev/notequest/   # tests
notehunt/src/main/resources/dev/notequest/config.json  # config
ARCHITECTURE.md                         # deep-dive architecture
ROADMAP.md                              # phased work items
LESSONS_LEARNED.md                      # agent-maintained issue/solution log
```

## Key Classes (one line each)
| Class | Purpose |
|---|---|
| `App.java` | Entry point; wires components |
| `FileWatcherService.java` | Background thread; monitors directory (currently disabled in App.java) |
| `FileIndexer.java` | Requests pending files from DB; writes to Lucene index |
| `DatabaseHandler.java` | H2 CRUD + EventBus subscriber |
| `FileTreeCrawler.java` | Recursive directory scan via FileVisitor |
| `EventBusRegistry.java` | Static Guava EventBus singleton |
| `ConfigProvider.java` | Loads config.json at startup; singleton |
| `FileResult.java` | DTO: path, status, lastModified, hash |
| `DatabaseQueries.java` | SQL constants (parameterized) |

## Database
**Table: `file_states`** — `File_Path`, `File_Path_Hash` (PK, MD5), `Status` (Pending/In_Progress/Complete/Error/Deleted), `Last_Modified`, `Error_Message`

## Configuration (`config.json`)
- `directoryPath` — root dir to monitor
- `indexPath` — Lucene index location (`%APPDATA%` replaced at runtime)
- `indexBatchSize` — files per indexing batch (default 50)

## Current Status
**Done:** FileWatcher, FileTreeCrawler, DB schema, Lucene indexing, EventBus, config, tests (46/52)
**Next:** REST API → query engine → snippet extraction (see ROADMAP.md Phase 1)

## Known Pitfalls
- FileWatcherService is disabled — do not enable without fixing single-JDBC-connection concurrency
- FileIndexerTest is `@Disabled` due to JVM memory issues — do not re-enable without profiling
- Windows paths use backslash; tests must account for `File.separator`
- H2 in-memory test DBs need unique names to avoid data pollution between tests
- `FileResult` with `ERROR` status may have `null` lastModified — null-check before use

## Subagent Instructions
1. **Read `LESSONS_LEARNED.md`** before starting work on any component
2. **When you resolve an issue**, append an entry to `LESSONS_LEARNED.md` under the matching section using this format:
   ```
   ### [Short title]
   **Issue:** What went wrong
   **Solution:** What fixed it
   ```
3. Scan existing entries before writing — do not duplicate
4. Create a new section if no existing section fits
```

**Step 2: Verify the file renders correctly**

Check that all sections are present and no section is bloated (>10 lines).

**Step 3: Commit**

```bash
git add CLAUDE.md
git commit -m "docs: add CLAUDE.md for subagent context"
```

---

### Task 2: Create LESSONS_LEARNED.md

**Files:**
- Create: `LESSONS_LEARNED.md`

**Step 1: Write the file pre-seeded with known issues**

```markdown
# Lessons Learned

Agent-maintained log. Append entries when you resolve an issue. Read this before starting work on any component.

---

## Testing

### Windows path separators in tests
**Issue:** Tests using hardcoded `/` path separators failed on Windows.
**Solution:** Use `File.separator` or `Path` objects instead of string literals for path construction.

### H2 in-memory DB name collisions
**Issue:** Multiple tests sharing the same in-memory DB name (`jdbc:h2:mem:testdb`) caused data leakage between tests.
**Solution:** Give each test class a unique DB name (e.g., `mem:databasehandlertest`, `mem:filewatchertest`).

### FileResult with ERROR status has null lastModified
**Issue:** `FileResult` objects created for error cases had `null` for `lastModified`, causing NullPointerExceptions in DB merge.
**Solution:** Null-check `lastModified` before use; treat null as epoch or skip the DB update.

### JUnit 5 uses @Disabled not @Ignore
**Issue:** `@Ignore` (JUnit 4) silently failed to disable tests after migrating to JUnit 5.
**Solution:** Replace `@Ignore` with `@Disabled` and add a reason string.

---

## FileIndexer

### FileIndexerTest causes JVM memory exhaustion
**Issue:** Running FileIndexerTest with real Lucene IndexWriter against a large corpus exhausted heap space.
**Solution:** Tests are `@Disabled` pending profiling. Use a small synthetic corpus (< 10 files) for unit tests; reserve large corpus for integration tests.

---

## Build / Maven

### JUnit 3 test runner incompatible with JUnit 5 tests
**Issue:** Default Maven Surefire with JUnit 3 dependency ignored all `@Test`-annotated methods in JUnit 5 style tests.
**Solution:** Upgrade `pom.xml` to JUnit Jupiter 5.x + Mockito + update Surefire plugin version to 2.22+.

---
```

**Step 2: Commit**

```bash
git add LESSONS_LEARNED.md
git commit -m "docs: add pre-seeded LESSONS_LEARNED.md for agent issue tracking"
```
