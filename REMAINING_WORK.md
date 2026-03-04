# NoteHunt: Remaining Work Summary

This document provides a quick overview of what work is needed to complete NoteHunt.

## Current State
- **Files monitored:** Yes (disabled in code)
- **Files indexed:** Yes (Lucene)
- **Index searchable:** No ❌
- **Results returned:** No ❌
- **API exposed:** No ❌
- **Full-featured:** No ❌

## Work Required to MVP

### 🔴 Critical (Blocking MVP)

#### 1. Search API Implementation
**What:** Expose HTTP endpoints for searching
**Why:** Users can't access functionality without API
**Effort:** 3-5 days
**Breakdown:**
- Add REST framework (Spring Boot or lightweight alternative)
- Create SearchController with `/search` endpoint
- Parse query parameters (q, limit, offset)
- Return JSON results

**Files to Create/Modify:**
- New: SearchController.java
- Modify: App.java (start HTTP server)
- Modify: pom.xml (add REST framework)

**Success Criteria:**
- `GET /search?q=hello` returns JSON array of results
- Results include file path and relevance score
- Pagination works (limit=10, offset=0)

#### 2. Query Processing
**What:** Convert user query strings to Lucene Query objects
**Why:** Lucene needs structured queries, not raw strings
**Effort:** 2-3 days
**Breakdown:**
- Wrap Lucene QueryParser
- Add query validation/sanitization
- Support boolean operators (AND, OR, NOT)
- Handle quote phrases

**Files to Create:**
- New: QueryParser.java (app wrapper)

**Success Criteria:**
- "hello world" → proper phrase/term query
- "java AND tutorial" → boolean AND query
- Rejects malformed queries with clear error

#### 3. Search Results Integration
**What:** Query Lucene index and return formatted results
**Why:** Currently no code queries the Lucene index
**Effort:** 3-4 days
**Breakdown:**
- Add IndexSearcher to FileIndexer
- Execute queries (currently IndexWriter only)
- Map results to SearchResult objects
- Implement pagination logic

**Files to Create/Modify:**
- New: SearchResult.java (DTO)
- Modify: FileIndexer.java (add search method)

**Success Criteria:**
- Search returns actual indexed documents
- Results include score, path, snippet preview
- Pagination doesn't return duplicate results

---

### 🟡 High Priority (Complete MVP)

#### 4. Snippet Extraction
**What:** Show matching text excerpt in results
**Why:** Users need context, not just file path
**Effort:** 3-5 days
**Breakdown:**
- Extract text around matched terms
- Highlight match (bold/color)
- Truncate long snippets gracefully

**Files to Create:**
- New: SnippetGenerator.java

**Success Criteria:**
- Results include 50-char context around match
- Matched terms highlighted
- Long files don't return full content

#### 5. Basic Error Handling
**What:** Don't crash on bad input or file errors
**Why:** Currently no validation or recovery
**Effort:** 2-3 days
**Breakdown:**
- Validate HTTP input (empty query, special chars)
- Catch file read errors gracefully
- Return proper HTTP error codes (400, 404, 500)
- Log errors properly

**Success Criteria:**
- Invalid queries return 400 Bad Request
- Missing files don't crash the app
- All exceptions logged with context

#### 6. Fix FileWatcherService
**What:** Re-enable and fix file monitoring
**Why:** Currently disabled; needed for real-time sync
**Effort:** 2-3 days
**Breakdown:**
- Uncomment in App.java
- Fix race conditions (concurrent DB writes)
- Add connection pooling for thread safety
- Test file monitoring actually works

**Files to Modify:**
- App.java (enable FileWatcherService)
- DatabaseHandler.java (add HikariCP or similar)

**Success Criteria:**
- FileWatcherService runs in background
- New files automatically detected
- Multiple file changes don't cause crashes

---

### 🟢 Medium Priority (Polish)

#### 7. Make File Extensions Configurable
**What:** Support more file types (not just .txt)
**Why:** Currently hardcoded, limits utility
**Effort:** 1-2 days

**Files to Modify:**
- config.json (add "fileExtensions": [".txt", ".md", ".pdf"])
- FileWatcherService.java (read from config)

#### 8. Add Metadata Support
**What:** Extract tags, titles from documents
**Why:** Better filtering and ranking
**Effort:** 4-6 days
**Breakdown:**
- Parse YAML front-matter
- Extract #tag syntax
- Store in database
- Index metadata fields in Lucene

**Files to Create:**
- New: MetadataExtractor.java
- New: Schema migration scripts

#### 9. Add Logging
**What:** Replace System.out/err with proper logging
**Why:** Can't debug without proper logs
**Effort:** 2-3 days

**Files to Modify:**
- All Java files (add logger calls)
- pom.xml (add Log4j2)

#### 10. Write Tests
**What:** Expand from 2 test classes to 15+
**Why:** Currently minimal test coverage
**Effort:** 5-7 days
**Breakdown:**
- Unit tests for each service
- Integration tests (index → search)
- API endpoint tests
- Edge case tests

**Success Criteria:**
- 70%+ code coverage
- Tests in CI/CD pipeline
- Zero test flakes

---

## Quick Wins (Easy, High Value)

These can be done in 30 min - 2 hours each:

1. **Add /health endpoint** (15 min)
   - GET /health → {"status": "ok"}

2. **Create Docker image** (1 hour)
   - Dockerfile + docker-compose.yml

3. **Document all commands** (1 hour)
   - README section with common commands

4. **Fix code comments** (30 min)
   - Fix "databse" typos
   - Add missing Javadoc

5. **Add build badge** (15 min)
   - Add GitHub Actions CI/CD
   - Add badge to README

6. **Create ISSUE_TEMPLATE.md** (30 min)
   - Standard bug/feature request template

---

## Simplified Roadmap

### Week 1: MVP (Search Works)
- [ ] REST API + SearchController
- [ ] Query Processing
- [ ] Search Results Integration
- [ ] Basic error handling

**Output:** Users can search indexed files via HTTP API

### Week 2: Polish (Actually Usable)
- [ ] Snippet generation
- [ ] Fix FileWatcherService
- [ ] Add logging
- [ ] Write 10+ tests

**Output:** Searchable with context; real-time file sync

### Week 3+: Features
- [ ] Tag support
- [ ] Result ranking
- [ ] Metadata extraction
- [ ] Comprehensive tests

**Output:** Full-featured search service

---

## Known Bugs to Fix

1. **File extensions hardcoded**
   - File: FileWatcherService.java:49-51
   - Fix: Read from config.json

2. **FileWatcherService disabled**
   - File: App.java:22
   - Fix: Uncomment + add thread management

3. **Single JDBC connection (not thread-safe)**
   - File: DatabaseHandler.java:37
   - Fix: Use HikariCP connection pool

4. **No input validation**
   - File: Will be SearchController
   - Fix: Validate queries before passing to Lucene

5. **Typos in comments**
   - File: DatabaseHandler.java:60 ("databse")
   - Fix: Spell-check all comments

---

## Effort Estimates

| Task | Days | Complexity | Priority |
|------|------|-----------|----------|
| REST API | 3-5 | High | Critical |
| Query Processing | 2-3 | Medium | Critical |
| Search Results | 3-4 | High | Critical |
| Snippets | 3-5 | Medium | High |
| Error Handling | 2-3 | Medium | High |
| Fix FileWatcher | 2-3 | Medium | High |
| Config File Extensions | 1-2 | Low | Medium |
| Metadata Support | 4-6 | High | Medium |
| Logging | 2-3 | Low | Medium |
| Tests | 5-7 | High | Medium |

**Total for MVP:** ~14-20 days (2-3 weeks)
**Total for 1.0:** ~30-40 days (6-8 weeks)

---

## Dependencies

### Critical Path (Blockers)
```
Query Processing ──┐
                  ├─→ Search Results ─→ REST API ─→ MVP
REST API Endpoint ┘
```

### Nice-to-Have (Parallel)
```
Logging ────────┐
Error Handling  ├─→ Stability
FileWatcher Fix ┘
```

### Future (Not Blocking)
```
Snippets ─┐
Ranking   ├─→ Polish
Metadata  ┘
```

---

## Success Metrics

**MVP (Week 1):**
- ✅ Can search via `GET /search?q=term`
- ✅ Returns JSON with file paths and scores
- ✅ Handles 1,000+ files in <200ms

**v1.0 (Week 3):**
- ✅ Can search with snippets showing context
- ✅ Real-time file monitoring works
- ✅ 70% test coverage
- ✅ Handles 10,000+ files in <500ms

---

**Last Updated:** 2024-03-04
