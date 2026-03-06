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

### SearchResultHandler needs IndexSearcher from FileIndexer
**Issue:** SearchConfiguration was passing null for IndexSearcher, causing NullPointerException at runtime when SearchResultHandler.executeSearch() tried to execute queries.
**Solution:** Added getSearcher() method to FileIndexer that opens/manages a cached DirectoryReader and IndexSearcher. Created IndexConfiguration to register FileIndexer as a Spring bean. Updated SearchConfiguration to inject FileIndexer and obtain IndexSearcher from it. This ensures all components share the same index and searcher is properly cached/refreshed.

---

## Build / Maven

### JUnit 3 test runner incompatible with JUnit 5 tests
**Issue:** Default Maven Surefire with JUnit 3 dependency ignored all `@Test`-annotated methods in JUnit 5 style tests.
**Solution:** Upgrade `pom.xml` to JUnit Jupiter 5.x + Mockito + update Surefire plugin version to 2.22+.

---

## FileWatcherService Concurrency

### Single JDBC connection causes race conditions with background file watcher
**Issue:** FileWatcherService runs on a background thread and posts file change events to EventBus. DatabaseHandler subscribers try to write to a single H2 JDBC connection simultaneously. JDBC connections are not thread-safe, causing deadlocks, data corruption, and crashes when multiple threads access the same connection concurrently.

**Solution:** Implemented HikariCP connection pool (max 10 connections, min 5 idle). Created ConnectionPoolProvider singleton with double-checked locking pattern (with volatile keyword for memory visibility). DatabaseHandler now gets a fresh connection from the pool for each database operation, enabling safe concurrent access.

**Key Learning:** JDBC connections are single-threaded. In multi-threaded systems, always use connection pooling (HikariCP, C3P0, etc.) instead of a single shared connection.

### WatchService doesn't detect new subdirectories
**Issue:** FileWatcherService registered directories at initialization time only (in constructor). If a new subdirectory was created after startup, it was never registered with WatchService, so file events in those new directories were silently dropped.

**Solution:** Added registerDirectory() method and dynamic directory registration. When ENTRY_CREATE events occur on directories, the new directory is automatically registered with WatchService, allowing events in newly created subdirectories to be tracked immediately.

**Key Learning:** File system monitoring must be dynamic. Assume directory structure changes after app startup and register new directories on the fly.

### Single exception in event handler crashes entire watch loop
**Issue:** startWatchingDirectory() had no exception handling for individual events. If bus.post() or any event processing threw an exception, the entire while loop crashed, stopping all file monitoring until app restart.

**Solution:** Added nested try-catch blocks:
- Outer try-catch for InterruptedException and fatal errors
- Middle try-catch for individual event processing
- Inner try-catch for bus.post() calls
- WatchKey validation to break gracefully if key becomes invalid
- All errors are logged but don't crash the loop, allowing resilience to temporary failures

**Key Learning:** Always wrap event processing loops in exception handlers. Single exceptions should not crash the entire monitoring system.

### WatchService resources not released
**Issue:** WatchService was never closed before application shutdown, causing potential memory leaks and resource exhaustion.

**Solution:** Added close() method to FileWatcherService that properly closes WatchService and logs the operation. Method includes error handling and can be called multiple times safely (idempotent). Should be called during application shutdown via a shutdown hook or dependency injection cleanup.

**Key Learning:** Always implement resource cleanup (close methods) for long-lived services. Use try-with-resources or explicit close() calls in shutdown handlers.

### Double-checked locking requires volatile keyword
**Issue:** ConnectionPoolProvider uses double-checked locking pattern for lazy initialization. Without the `volatile` keyword on the instance variable, the Java Memory Model doesn't guarantee that writes inside the synchronized block are visible to reads outside it. This can cause threads to see partially-initialized HikariDataSource objects.

**Solution:** Added `volatile` keyword to the instance variable:
```java
private static volatile HikariDataSource instance;
```

**Key Learning:** Double-checked locking in Java requires `volatile` on the instance variable for memory visibility guarantees. This is a subtle but critical concurrency pattern that's easy to miss.

### Connection cleanup requires finally blocks
**Issue:** HikariCP connection pool exhaustion (timeout after 30 seconds, all 10 connections active, 0 idle). Root cause: 5 DatabaseHandler methods acquired connections but never returned them on exception paths:
- `getStatusCounts()` - called by `/index/status` REST endpoint (repeated by frontend)
- `getLastSyncTime()` - also called by `/index/status`
- `fetchPendingFiles()` - called during file indexing
- `mergeFilesIntoTable()` - called for file state updates
- `flagStaleFilesInDirectory()` - called during directory crawl

If any threw SQLException, the connection leaked indefinitely. Multiple concurrent HTTP requests quickly exhausted the 10-connection pool.

**Solution:** Added finally blocks to all 5 methods to guarantee connection cleanup:
```java
Connection conn = null;
try {
    conn = getConnection();
    // operation here
} catch (SQLException e) {
    throw new RuntimeException(..., e);
} finally {
    if (testConnection == null && conn != null) {
        try { conn.close(); } catch (SQLException ignored) {}
    }
}
```

Verified: testDatabaseConnection() and setupSchema() already had finally blocks and worked correctly; all 5 vulnerable methods now fixed.

**Key Learning:** Connection cleanup must be in finally blocks or try-with-resources, not in try or catch blocks. This is especially critical with connection pooling where leaked connections exhaust the pool and cause cascading timeouts.

---
