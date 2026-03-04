# Design: FileWatcherService Fixes and Re-enable FileIndexerTest

**Date:** 2026-03-04
**Status:** Approved

## Summary

Fix three bugs in `FileWatcherService` and re-enable the six disabled `FileIndexerTest` tests by fixing a resource leak in `FileIndexer`.

---

## Bug 1 — Null EventBus in default constructor

**File:** `FileWatcherService.java:70`

The default constructor assigns `this.bus = bus`, which is a self-assignment of the uninitialized field (evaluates to `null`). Any call to `bus.post(...)` after construction will throw a `NullPointerException`.

**Fix:** Replace with `this.bus = EventBusRegistry.bus();`

---

## Bug 2 — Wrong path resolution for subdirectory events

**File:** `FileWatcherService.java:155`

```java
Path full = dirPath.resolve(event.context().toString());
```

`WatchEvent.context()` returns a filename relative to the **WatchKey's watched directory**, not the root `dirPath`. For a file at `/root/subdir/file.txt`, the current code produces `/root/file.txt` (wrong).

**Fix:** Resolve against the WatchKey's own directory:

```java
Path watchedDir = (Path) key.watchable();
Path full = watchedDir.resolve((Path) event.context());
```

---

## Bug 3 — Newly created subdirectories not registered

**File:** `FileWatcherService.java` — `startWatchingDirectory()`

The constructor registers all directories at startup, but `WatchService` does not automatically track directories created after registration. Events inside a new subdirectory are silently missed.

**Fix:** In the event loop, when `ENTRY_CREATE` fires for a directory, register it with the `WatchService`.

---

## Bug 4 — IndexWriter not closed in FileIndexer (resource leak)

**File:** `FileIndexer.java`

`FileIndexer` creates a Lucene `IndexWriter` but never exposes a way to close it. In `FileIndexerTest`, `tearDown` does not close the writer, creating a resource leak across tests. This was the likely cause of JVM instability that prompted `@Disabled`.

**Fix:** Add a `close()` method to `FileIndexer` that closes the `IndexWriter`. Update `FileIndexerTest.tearDown()` to call it.

---

## FileIndexerTest re-enablement

Remove the `@Disabled` annotation from `FileIndexerTest`. No test logic changes — the six existing tests are correctly written and should pass once the resource leak is resolved.

---

## Files Changed

| File | Change |
|------|--------|
| `FileWatcherService.java` | Fix null bus, path resolution, new subdir registration |
| `FileIndexer.java` | Add `close()` method |
| `FileIndexerTest.java` | Remove `@Disabled`, add cleanup in `tearDown` |
