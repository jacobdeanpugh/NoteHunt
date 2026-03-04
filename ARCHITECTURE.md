# NoteHunt Architecture Documentation

Deep dive into system design, component interactions, and design decisions.

## System Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                     NoteHunt System                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │         HTTP Server (REST API) [FUTURE]                │  │
│  │  /search, /health, /index/status, /tags                │  │
│  └──────────────┬───────────────────────────────────────────┘  │
│                 │                                              │
│  ┌──────────────▼───────────────────────────────────────────┐  │
│  │              Event Bus (Guava EventBus)                 │  │
│  │  Async message passing between components               │  │
│  └──────────────┬────────────────────────────┬──────────────┘  │
│                 │                            │                 │
│  ┌──────────────▼─────────┐   ┌──────────────▼────────────┐  │
│  │  File Watcher Service  │   │   Database Handler        │  │
│  │  - Monitors files      │   │   - Stores file state     │  │
│  │  - Publishes events    │   │   - Tracks indexing status│  │
│  │  (CURRENTLY DISABLED)  │   │   - H2 SQL queries        │  │
│  └───────────────────────┘   └──────────────┬────────────┘  │
│                                              │                │
│  ┌──────────────────────────────────────────▼──────────────┐  │
│  │         File System / Database                         │  │
│  │  ┌──────────────────────┐  ┌──────────────────────┐   │  │
│  │  │  Directory Tree      │  │  H2 Database         │   │  │
│  │  │  .txt files          │  │  file_states table   │   │  │
│  │  └──────────────────────┘  └──────────────────────┘   │  │
│  └──────────────────────────────────────────────────────────┘  │
│                 │                                              │
│  ┌──────────────▼───────────────────────────────────────────┐  │
│  │           File Indexer                                  │  │
│  │  - Reads from database                                  │  │
│  │  - Batches files into Lucene index                      │  │
│  │  - Publishes completion events                          │  │
│  └──────────────┬───────────────────────────────────────────┘  │
│                 │                                              │
│  ┌──────────────▼───────────────────────────────────────────┐  │
│  │        Apache Lucene Index                              │  │
│  │  Full-text search engine                               │  │
│  │  (Currently built but never queried)                   │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## Component Details

### 1. Application Entry Point (App.java)

**Responsibility:** Initialize and orchestrate system startup.

**Current Flow:**
1. Create DatabaseHandler (sets up H2, schema)
2. Register DatabaseHandler with EventBus
3. Create FileIndexer and request pending files
4. Print results to stderr

**Why FileWatcherService is Disabled:**
- Comment indicates incomplete feature
- File watching without search API is incomplete
- Focus shifted to completing indexing pipeline

**Future:** Launch both FileWatcherService thread and HTTP server

---

### 2. Event Bus (Guava EventBus)

**Pattern:** Publisher-Subscriber for decoupled async communication.

**Registry:** `EventBusRegistry.java` maintains static singleton instance.

**Events:**
- `FileChangeEvent` - Single file created/modified/deleted
- `FileTreeCrawledEvent` - Directory scan completed
- `PendingFilesRequestEvent` - Async query for pending files
- `SetFilesToCompleteEvent` - Mark batch as indexed

**Subscriber Pattern:**
```java
@Subscribe
public void handleFileChangeEvent(FileChangeEvent event) {
    // Automatically called when event posted to bus
}
```

**Advantages:**
- Loose coupling between file monitoring and database
- Enables parallel processing of multiple file changes
- Easy to add new event handlers without modifying existing code

**Disadvantages:**
- No built-in error handling if subscriber throws exception
- Events are lost if no subscriber registered
- Difficult to debug event flow in logs

---

### 3. File Watcher Service (FileWatcherService.java)

**Responsibility:** Monitor filesystem for file changes.

**Implementation:**
- Extends `Thread` (runs in background)
- Uses Java `WatchService` API with `FileSystems.getDefault()`
- Registers all directories recursively at startup (`Files.walk()`)
- Continuously polls for events in `startWatchingDirectory()`

**Event Types Monitored:**
- `ENTRY_CREATE` - New file added
- `ENTRY_MODIFY` - File content/metadata changed
- `ENTRY_DELETE` - File removed

**File Filtering:**
- Extension-based filtering (currently: `.txt` only)
- Hardcoded in `FILE_EXTENSIONS` constant
- Could be made configurable via config.json

**Lifecycle:**
1. Constructor registers all directories with WatchService
2. `run()` method (Thread entry) calls `getAllDirectoryFiles()` then `startWatchingDirectory()`
3. `getAllDirectoryFiles()` triggers initial FileTreeCrawler scan
4. `startWatchingDirectory()` blocks until events occur

**Known Issues:**
- Single-threaded event processing (may miss rapid consecutive events)
- Recursive directory registration happens at init time (new subdirs not watched)
- Exception in event handler crashes entire watching loop
- Memory leak possible if WatchService not properly cleaned up on shutdown

---

### 4. File Tree Crawler (FileTreeCrawler.java)

**Responsibility:** Recursively scan directory tree at startup.

**Implementation:**
- Implements `FileVisitor` interface (used with `Files.walkFileTree()`)
- Traverses directory recursively without maintaining recursive state
- Builds `FileResult` for each file matching extension filter

**Output:** `FileTreeCrawledEvent` published with all discovered files.

**Advantages:**
- Clean file visitor pattern for recursion
- Single pass through directory tree
- Works with large directories

---

### 5. File Result DTO (FileResult.java)

**Responsibility:** Encapsulate file metadata for passing between components.

**Data:**
- `Path` - file path
- `FileStatus` - Pending, In_Progress, Complete, Error, Deleted
- `FileTime` - last modified timestamp
- Exception details (if error occurred)
- `getFilePathHash()` - MD5 hash of path for database lookups

**File Status Lifecycle:**
```
[Discovered]
    ↓
PENDING → IN_PROGRESS → COMPLETE
             ↓
            ERROR (with exception message)

[Later]
    ↓
DELETED (marked when file no longer exists)
```

---

### 6. Database Handler (DatabaseHandler.java)

**Responsibility:** Persist file states and manage database operations.

**Database:** H2 embedded (JDBC: `jdbc:h2:file:./data/db`)

**Schema:**
```sql
CREATE TABLE file_states (
  File_Path       VARCHAR(1024) NOT NULL,
  File_Path_Hash  VARCHAR(32) PRIMARY KEY,
  Status          VARCHAR(15) CHECK (Status IN (...)),
  Last_Modified   DATETIME NOT NULL,
  Error_Message   VARCHAR
)
```

**Key Operations:**

1. **mergeFilesIntoTable()** - Upsert operation
   - Uses SQL MERGE statement
   - Only updates if file newer than DB version
   - Batch processing for efficiency
   - Updates trigger database completion events

2. **flagStaleFilesInDirectory()** - Mark deleted files
   - Find files in DB but not in current scan
   - Marks them with `DELETED` status
   - Prevents orphaned records

3. **fetchPendingFiles()** - Query for files to index
   - Returns all files with `PENDING` status
   - Called by FileIndexer to get batch

4. **Event Subscribers:**
   - `handleFileTreeCrawledEvent()` - Full directory scan results
   - `handleFileChangeEvent()` - Single file changes
   - `handlePendingFilesRequest()` - Query for pending files (async)
   - `handleSetFilesToCompleteEvent()` - Mark batch as indexed

**Design Pattern:** Event handler pattern allows decoupled updates. FileWatcher publishes events; DatabaseHandler subscribes.

**Thread Safety Issues:**
- Single JDBC Connection (not thread-safe)
- Multiple threads may call mergeFilesIntoTable() concurrently
- Future: Use connection pool (HikariCP) or single writer thread

---

### 7. File Indexer (FileIndexer.java)

**Responsibility:** Build and maintain Apache Lucene full-text index.

**Components:**
- `StandardAnalyzer` - Text tokenization/stemming
- `FSDirectory` - Index stored on disk
- `IndexWriter` - Writes documents to index
- `IndexWriterConfig` - CREATE_OR_APPEND mode (non-destructive)

**Workflow:**

1. **Construction:**
   - Opens FSDirectory pointing to `indexPath` from config
   - Creates StandardAnalyzer for English text
   - Initializes IndexWriter in append mode

2. **indexFile(Path):**
   - Reads file content
   - Creates Lucene Document with fields:
     - `path` - StringField (exact match, stored)
     - `contents` - TextField (analyzed, not stored)
   - Adds document to writer (in-memory buffer)

3. **indexFilesFromDatabase():**
   - Requests pending files from database via event
   - Batches by `indexBatchSize` (50 files)
   - For each batch:
     - Calls `indexFile()` for each file
     - Commits IndexWriter
     - Posts `SetFilesToCompleteEvent` to mark as indexed

4. **requestPendingFiles():**
   - Publishes `PendingFilesRequestEvent` to EventBus
   - Waits for response via CompletableFuture
   - Exception-safe (returns empty list on timeout/error)

**Design Notes:**
- Lucene index written but never queried (FUTURE: implement search)
- No incremental updates (rebuilds index each time)
- No optimization/merging step
- Files stored on disk but in-memory buffers not flushed until commit

---

### 8. Configuration Provider (ConfigProvider.java)

**Responsibility:** Load and provide configuration at runtime.

**Singleton Pattern:**
```java
public static final ConfigProvider instance = new ConfigProvider();
```

**Configuration Source:**
- File: `src/main/resources/dev/notequest/config.json`
- Loaded on first class load

**Parameters:**
- `directoryPath` - Root directory to monitor
- `indexPath` - Where indices stored (replaces %APPDATA% with env var)
- `indexBatchSize` - Files per batch during indexing

**Error Handling:**
- Throws RuntimeException if file not found or parse fails
- Happens at startup (fail fast)

---

### 9. Database Queries (DatabaseQueries.java)

**Responsibility:** Centralize SQL query strings.

**Query Categories:**

1. **Schema Setup:**
   - `SETUP_SCHEMA` - CREATE TABLE IF NOT EXISTS

2. **Insert/Update:**
   - `STANDARD_MERGE_INTO_TABLE` - Upsert with conditional logic

3. **Status Updates:**
   - `MARK_FILES_AS_DELETED` - Set status = 'Deleted'
   - `MARK_FILES_AS_PENDING` - Set status = 'Pending'
   - `FLAG_STALE_FILES_IN_DIRECTORY` - Mark deleted files

4. **Queries:**
   - `SELECT_PENDING_FILES` - Get files awaiting indexing
   - `REMOVE_FILES_FROM_TABLE` - Delete records

**Design Patterns:**
- SQL constants (not code strings)
- Uses parameterized queries (? placeholders)
- Prevents SQL injection
- H2-specific syntax (may not port to PostgreSQL/MySQL)

---

## Data Flow Diagrams

### Startup Sequence

```
App.main()
    ├─ new DatabaseHandler()
    │   ├─ Create ./data directory
    │   ├─ Connect to H2 database
    │   ├─ Execute SETUP_SCHEMA
    │   └─ Register with EventBus
    │
    ├─ new FileIndexer()
    │   ├─ Open FSDirectory
    │   ├─ Create StandardAnalyzer
    │   └─ Create IndexWriter
    │
    ├─ FileIndexer.requestPendingFiles()
    │   ├─ Post PendingFilesRequestEvent to bus
    │   ├─ DatabaseHandler receives & processes
    │   ├─ Queries SELECT_PENDING_FILES
    │   └─ Returns results via CompletableFuture
    │
    └─ FileIndexer.indexFilesFromDatabase()
        ├─ For each batch of files:
        │   ├─ indexFile() for each
        │   ├─ IndexWriter.commit()
        │   └─ Post SetFilesToCompleteEvent
        └─ [Indexing complete]
```

### File Change Event Flow

```
FileWatcherService.startWatchingDirectory()
    ├─ WatchService.take() blocks until event
    ├─ Creates FileResult from event
    ├─ Checks file extension filter
    │
    └─ Post FileChangeEvent to EventBus
        └─ DatabaseHandler.handleFileChangeEvent()
            └─ mergeFilesIntoTable()
                ├─ MERGE INTO file_states
                └─ Sets status = PENDING
```

### Index Query Flow [FUTURE]

```
HTTP GET /search?q=python
    ├─ SearchController parses query
    ├─ QueryParser converts to Lucene Query
    │
    ├─ FileIndexer.executeQuery()
    │   ├─ IndexSearcher.search(query, limit)
    │   ├─ Iterate TopDocs hits
    │   └─ Build SearchResult objects
    │
    └─ Return JSON results
        ├─ File path
        ├─ Relevance score
        ├─ Snippet with highlights
        └─ Metadata
```

---

## Design Patterns Used

1. **Event Bus (Observer)** - Decoupled event publishing/subscribing
2. **Data Transfer Object (DTO)** - FileResult carries data between layers
3. **Singleton** - ConfigProvider, EventBusRegistry
4. **Builder Pattern** - IndexWriterConfig
5. **Thread** - FileWatcherService extends Thread
6. **Factory** - FileTreeCrawler creates FileResults

---

## Key Dependencies & Justification

| Dependency | Version | Purpose | Alternatives |
|-----------|---------|---------|--------------|
| Apache Lucene | 10.3.1 | Full-text indexing | Elasticsearch (too heavy), Whoosh (Python) |
| H2 Database | 2.3.232 | File state persistence | SQLite, PostgreSQL (requires server) |
| Google Guava | 31.1 | EventBus, utilities | Spring's ApplicationEventPublisher, custom |
| json-simple | 1.1.1 | JSON config parsing | Jackson, GSON |
| JUnit | 3.8.1 | Testing | JUnit 5, TestNG |

---

## Concurrency Considerations

**Current Issues:**
- FileWatcherService (Thread) and main thread may call EventBus simultaneously
- DatabaseHandler has single JDBC connection (not thread-safe)
- IndexWriter is thread-safe but only one should be active

**Future Mitigations:**
1. Use connection pool (HikariCP) for database
2. Implement single writer queue (new events to queue, single thread processes)
3. Add synchronization for file_states table updates
4. Use read-write locks for Lucene index (multiple readers, single writer)

---

## Performance Characteristics

**Indexing:**
- Batch size: 50 files (configurable)
- Commit after each batch (synchronous)
- Time complexity: O(n) where n = files to index
- Space: Lucene index on disk + in-memory writer buffer

**Database:**
- H2 embedded (single-threaded)
- MERGE statement avoids full update
- Batch inserts for efficiency
- No indices on Status or Last_Modified (slow scans)

**File Watching:**
- WatchService efficient (uses OS events)
- Recursive registration at startup only
- Polling loop may introduce latency

**Index Querying:**
- Future bottleneck (not yet implemented)
- Lucene supports efficient queries but no caching yet

---

## Known Limitations & Future Work

| Limitation | Severity | Impact | Solution |
|-----------|----------|--------|----------|
| No REST API | High | Can't query index | Implement Phase 1 |
| FileWatcher disabled | High | No real-time sync | Enable + fix concurrency |
| Single JDBC connection | Medium | Concurrency issues | Use HikariCP pool |
| Hardcoded .txt filter | Medium | Can't index other types | Make configurable |
| No error recovery | Medium | Index corruption on crash | Implement checksums |
| Memory limits unknown | Low | May OOM on large collections | Implement profiling |

---

**Last Updated:** 2024-01
