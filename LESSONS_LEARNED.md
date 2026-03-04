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
