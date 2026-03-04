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
- `indexBatchSize` — files per batch during indexing (default 50)

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
