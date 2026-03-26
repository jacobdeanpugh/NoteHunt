# NoteHunt (NoteQuest)

A personal knowledge-base search service that monitors your note directories and keeps a full-text search index synchronized in real-time.

## Overview

NoteHunt is a backend service designed for students, researchers, and writers who want powerful full-text search over their personal note collection. Drop a folder of Markdown, plain text, or other documents into a monitored directory, and instantly search with ranking, filtering, and snippet extraction—all running locally on embedded databases.

**Key Features:**
-  **Automatic File Monitoring** - Detects file creation, modification, and deletion
-  **Full-Text Search** - Apache Lucene-based indexing and querying
-  **Persistent State** - H2 embedded database tracks file states and metadata
-  **Efficient Sync** - Batch processing with incremental updates
-  **Lightweight** - Designed for modest hardware (4-8 GB RAM)

## Project Status

### ✅ Completed Components
- **File System Monitoring** - FileWatcherService monitors directories recursively
- **File Crawling** - FileTreeCrawler performs initial directory scan at startup
- **Event System** - Guava EventBus handles asynchronous file change events
- **Database Schema** - H2 database tracks file paths, hashes, and indexing status
- **Indexing Engine** - Apache Lucene integration for full-text indexing
- **Configuration System** - JSON-based configuration loader

### ⏳ In Progress / Incomplete
- **REST API** - No HTTP endpoints exposed yet for search queries
- **Search Query Handler** - No query processing logic implemented
- **Result Ranking** - Basic Lucene scoring only, no custom ranking
- **Snippet Generation** - No context preview extraction
- **Metadata Filtering** - Tag and category filtering not implemented
- **Error Recovery** - Limited handling for corrupted index states

## Architecture

### Core Components

```
┌─────────────────────────────────────────────────────────┐
│                    App.java                             │
│              (Application Entry Point)                  │
└────────────────┬────────────────────────────────────────┘
                 │
                 ├─────────────────────────┬───────────────┐
                 │                         │               │
        ┌────────▼────────────┐  ┌────────▼──────┐  ┌─────▼──────────┐
        │ FileWatcherService  │  │ FileIndexer   │  │ DatabaseHandler│
        │  (File Monitoring)  │  │  (Lucene)     │  │   (H2 DB)      │
        └────────┬────────────┘  └────────┬──────┘  └─────┬──────────┘
                 │                        │              │
                 │    FileTreeCrawler     │              │
                 └────────────┬───────────┘              │
                              │                          │
                    ┌─────────▼──────────────┬───────────┘
                    │                        │
              ┌─────▼────────┐    ┌─────────▼──────┐
              │  Event Bus   │    │  File States   │
              │  (GuavaEB)   │    │   Database     │
              └──────────────┘    └────────────────┘
```

### Data Flow

1. **Startup**: App initializes DatabaseHandler, registers event listeners, calls FileIndexer
2. **Initial Scan**: FileIndexer calls FileTreeCrawler to recursively scan directory
3. **Database Sync**: FileWatcherService publishes file crawl results; DatabaseHandler updates file_states table
4. **Monitoring**: FileWatcherService watches for file system events (create/modify/delete)
5. **Event Handling**: File change events trigger database updates via event handlers
6. **Indexing**: FileIndexer processes pending files from database and writes to Lucene index

### Database Schema

**Table: `file_states`**
| Column | Type | Purpose |
|--------|------|---------|
| `File_Path` | VARCHAR(1024) | Full path to the file |
| `File_Path_Hash` | VARCHAR(32) | MD5 hash of path (primary key) |
| `Status` | VARCHAR(15) | Indexing status: Pending, In_Progress, Complete, Error, Deleted |
| `Last_Modified` | DATETIME | File's last modification time |
| `Error_Message` | VARCHAR | Exception details if status is Error |

**File Status Lifecycle:**
- `Pending` → File needs indexing
- `In_Progress` → Currently being indexed
- `Complete` → Successfully indexed
- `Error` → Failed to index (see Error_Message)
- `Deleted` → Marked for removal (not currently active in index)

### Event System

Uses Google Guava EventBus for asynchronous event handling:
- `FileChangeEvent` - Single file created/modified/deleted
- `FileTreeCrawledEvent` - Directory scan completed
- `PendingFilesRequestEvent` - Query for files awaiting indexing
- `SetFilesToCompleteEvent` - Mark batch of files as indexed

## Setup & Configuration

### Prerequisites
- Java 21+
- Maven 3.6+

### Configuration

Edit `notehunt/src/main/resources/dev/notequest/config.json`:

```json
{
  "directoryPath": "C:\\Users\\jacob\\Downloads\\20_newsgroups\\talk.politics.misc",
  "indexPath": "%APPDATA%\\NoteQuest\\index\\",
  "indexBatchSize": 50
}
```

**Parameters:**
- `directoryPath` - Root directory to monitor for changes
- `indexPath` - Where Lucene indices are stored (%APPDATA% is replaced at runtime)
- `indexBatchSize` - Number of files to process per indexing batch

### Building

```bash
cd notehunt
mvn clean package
```

This creates a shaded JAR at `target/notehunt-1.0-SNAPSHOT.jar` with all dependencies.

### Running

```bash
java -cp target/notehunt-1.0-SNAPSHOT.jar dev.notequest.App
```

## Dependencies

- **Apache Lucene 10.3.1** - Full-text search indexing
- **H2 Database 2.3.232** - Embedded relational database
- **Google Guava 31.1** - Event bus and utilities
- **json-simple 1.1.1** - JSON configuration parsing
- **JUnit 3.8.1** - Testing framework

## File Structure

```
notehunt/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/dev/notequest/
│   │   │   ├── App.java                    (Entry point)
│   │   │   ├── handler/
│   │   │   │   ├── DatabaseHandler.java    (H2 operations)
│   │   │   │   └── EventBusRegistry.java   (Event management)
│   │   │   ├── service/
│   │   │   │   ├── FileWatcherService.java (Directory monitoring)
│   │   │   │   ├── FileIndexer.java        (Lucene indexing)
│   │   │   │   ├── FileResult.java         (File metadata DTO)
│   │   │   │   └── FileTreeCrawler.java    (Recursive directory scan)
│   │   │   ├── models/
│   │   │   │   └── DatabaseQueries.java    (SQL queries)
│   │   │   ├── events/
│   │   │   │   ├── FileChangeEvent.java
│   │   │   │   ├── FileTreeCrawledEvent.java
│   │   │   │   ├── PendingFilesRequestEvent.java
│   │   │   │   └── SetFilesToCompleteEvent.java
│   │   │   └── util/
│   │   │       ├── ConfigProvider.java     (Config loader)
│   │   │       └── MD5Util.java           (Path hashing)
│   │   └── resources/
│   │       └── dev/notequest/
│   │           └── config.json
│   └── test/
│       └── java/dev/notequest/
│           ├── AppTest.java
│           └── service/FileWatcherServiceTest.java
└── target/                                 (Build output)
```

## Known Issues & Limitations

1. **No Search API** - Currently no HTTP endpoints. FileIndexer only logs output to console.
2. **Limited File Types** - Only monitors `.txt` files (hardcoded in FileWatcherService)
3. **No Metadata Extraction** - Doesn't parse YAML front-matter or tags
4. **Index Not Queried** - Lucene index is built but never searched
5. **No Recovery Mechanism** - Crashes leave index in inconsistent state
6. **Hardcoded Filter** - File extensions are constants, not configurable

## Next Steps / Roadmap

See [ROADMAP.md](./ROADMAP.md) for detailed work items and priority breakdown.

Quick summary:
1. **Implement Search API** - REST endpoints for querying
2. **Query Processing** - Parse user queries, build Lucene queries
3. **Result Ranking** - Implement scoring, pagination, relevance feedback
4. **Metadata Support** - Extract tags, categories, dates from content
5. **Snippet Generation** - Show matched context in results
6. **Error Handling** - Graceful degradation on index corruption
7. **Performance Tuning** - Optimize batch sizes, cache strategies

## Testing

Run tests with:
```bash
mvn test
```

Current test coverage is minimal. Tests exist for FileWatcherService initialization.

## License

Not specified in project files.
