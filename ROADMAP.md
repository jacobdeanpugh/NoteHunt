# NoteHunt Development Roadmap

This document outlines the work needed to complete NoteHunt from current state to a fully functional personal search service.

## Current State Summary

**What Works:**
- ✅ File system monitoring (FileWatcherService)
- ✅ Initial directory scanning (FileTreeCrawler)
- ✅ Event-driven architecture (Guava EventBus)
- ✅ File state tracking (H2 database)
- ✅ Apache Lucene indexing setup
- ✅ Configuration management

**What's Missing:**
- ❌ Web dashboard UI
- ❌ Error recovery
- ❌ Performance optimization

---

## Phase 1: Search API Foundation (High Priority) — ✅ COMPLETED

**Status:** Implemented. REST endpoints for search, query processing, and results handler.

### 1.1 Implement REST Controller
**Status:** ✅ Complete
Expose search endpoints via REST framework.

**Endpoints:**
- `GET /search?q=<query>` - Basic text search
- `GET /search?q=<query>&limit=10&offset=0` - Pagination support
- `GET /health` - Service health check
- `GET /index/status` - Indexing progress/stats

---

### 1.2 Query Processing Engine
**Status:** ✅ Complete
Convert user queries into Lucene QueryParser queries.

**Features:**
- Parse simple text queries
- Support boolean operators (AND, OR, NOT)
- Quote phrases
- Field targeting (title:, content:, etc.)
- Query validation and sanitization

---

### 1.3 Search Results Handler
**Status:** ✅ Complete
Execute queries, return formatted results with metadata.

**Features:**
- Execute Lucene Query on IndexSearcher
- Return top N results (paginated)
- Include relevance score
- Include file path and metadata
- Support sorting (by relevance, date, filename)

---

## Phase 2: Result Enhancement (Medium Priority) — ✅ COMPLETED

### 2.1 Snippet/Context Extraction
**Status:** ✅ Complete
Extract relevant text excerpts showing matched terms in context.

**Features:**
- Highlight matched query terms in result text
- Extract surrounding context
- Handle multiple matches per file
- Truncate long snippets gracefully

---

### 2.2 Result Ranking & Relevance
**Status:** ✅ Complete
Improve result ranking beyond basic Lucene scoring.

**Strategies:**
- Boost file modification recency
- Boost file size/richness
- Metadata weighting

---

## Phase 3: Web Dashboard UI (High Priority)

### 3.1 React SPA Frontend
**Status:** Not started
**Dependencies:** Phase 1, Phase 2
**Time Estimate:** 4-6 days

Build a modern, minimal web dashboard following Acme-inspired design (warm dark, editorial aesthetic, no gradients/glow/shadows).

**Design Principles:**
- Background: #0e0e0e (warm near-black)
- Surface: #141414 with #1e1e1e borders
- Typography: Geist/Inter with bold/light weight mixing
- Sidebar: Icon-only (~48px), no labels
- Flat design: depth via typography weight, not shadows

**Required Features:**

**Screen 1: Search (Home)**
- Greeting with system username: "Good morning, [Name]"
- 4 summary cards: Recent Files, Index Status, Top Tags, Last Search
- Conversational search input with prompt starters
- Search results grid: file path, relevance score, snippet, tags, modification date
- Pagination support (20 results per page)

**Screen 2: Index Status**
- 4 stat cards: Pending count, In Progress count, Error count, Last synced
- Paginated file table (monospace paths, status as plain text)
- Re-index button (text link, not styled button)

**Screen 3: Settings**
- Configuration form: directory path, index path, batch size
- File extension toggles (.txt, .md, .rst, .org)
- Save button (only light element: #f5f5f5 bg, #0e0e0e text)

**Deliverables:**
- [ ] Set up React 18 + Vite project
- [ ] Create Tailwind CSS config with Acme color palette
- [ ] Implement Sidebar + TopBar components (shared across screens)
- [ ] Build Screen 1: Search with results display
- [ ] Build Screen 2: Index Status with file table
- [ ] Build Screen 3: Settings (read-only initially, writeable in Phase 5)
- [ ] Integrate with Phase 1 `/search` endpoint
- [ ] Integrate with Phase 1 `/index/status` endpoint
- [ ] Integrate with Phase 2 snippet and ranking data
- [ ] Add pagination for search results and file table
- [ ] Add error handling (API failures, empty results)
- [ ] Write component tests for critical UI logic
- [ ] Document dev setup and build process

**API Integration Points:**
- `GET /search?q=<query>&limit=20&offset=0` — Search results with snippets (Phase 1.3 + 2.1)
- `GET /index/status` — Index statistics (Phase 1.1)
- Results include: relevance score (Phase 2.2), snippets (Phase 2.1), tags (Phase 4.1 metadata when available)

**Styling Notes:**
- No box shadows, no rounded corners > 6px
- Card borders: 1px solid #1e1e1e
- Active sidebar icon: faint #1e1e1e pill background, no color accent
- File status colors: muted for Complete, yellow for Pending, red for Error
- Monospace for file paths (Geist Mono, 12px)

---

## Phase 4: Content Metadata & Filtering (Medium Priority)

### 4.1 Metadata Extraction
**Status:** Not started
**Dependencies:** None (parallel to Phase 1)
**Time Estimate:** 4-6 days

Extract and index metadata from documents.

**Metadata to Support:**
- **YAML Front Matter** (common in Markdown)
  ```yaml
  ---
  title: My Note
  tags: [python, tutorial]
  date: 2024-01-15
  category: programming
  ---
  ```
- **Tags/Keywords** (#tag syntax, inline keywords)
- **Creation/Modification Dates** (from file system)
- **Document Title** (first heading or YAML title field)
- **Author/Source** (optional metadata)

**Deliverables:**
- [ ] Create MetadataExtractor utility
- [ ] Implement YAML front-matter parser (use SnakeYAML or similar)
- [ ] Extract tags from document text (regex: #[a-z]+)
- [ ] Store metadata in database (expand file_states schema)
- [ ] Index metadata fields in Lucene (title:, tag:, date:, etc.)
- [ ] Update FileIndexer to extract and index metadata
- [ ] Write tests for various document formats

**Database Schema Changes:**
```sql
ALTER TABLE file_states ADD COLUMN title VARCHAR(256);
ALTER TABLE file_states ADD COLUMN tags VARCHAR(512);
ALTER TABLE file_states ADD COLUMN date_indexed DATETIME;
ALTER TABLE file_states ADD COLUMN created_date DATETIME;
```

---

### 4.2 Tag/Category Filtering
**Status:** Not started
**Dependencies:** 4.1
**Time Estimate:** 2-3 days

Allow users to filter search results by tags.

**Required Features:**
- API endpoint: `GET /search?q=python&tag=tutorial`
- Support multiple tag filters (AND logic: must have all tags)
- List available tags: `GET /tags`
- Tag auto-complete/suggestions

**Deliverables:**
- [ ] Add tag filtering to QueryParser (convert tag: field to AND clause)
- [ ] Implement `/tags` endpoint returning tag list with counts
- [ ] Add tag filter validation
- [ ] Write integration tests (search with tag filters)

---

## Phase 5: Infrastructure & Polish (Lower Priority)

### 5.1 Error Handling & Recovery
**Status:** Partially implemented (basic try-catch)
**Dependencies:** None
**Time Estimate:** 4-5 days

Improve resilience and graceful degradation.

**Scenarios to Handle:**
- **Corrupted Lucene Index** - Rebuild on startup if invalid
- **Database Lock** - Retry with exponential backoff
- **Out of Memory** - Reduce batch sizes, increase GC logging
- **Disk Full** - Pause indexing, alert user
- **Interrupted Indexing** - Resume from last successful state
- **Bad Files** - Skip files that cause parsing errors

**Deliverables:**
- [ ] Add index validation on startup
- [ ] Implement index rebuild fallback
- [ ] Add retry logic for database operations
- [ ] Graceful error pages/responses for HTTP errors
- [ ] Comprehensive logging (Log4j, SLF4j)
- [ ] Add monitoring/observability (metrics, structured logs)

---

### 5.2 Performance Optimization
**Status:** Not analyzed
**Dependencies:** 1.3 (need baseline first)
**Time Estimate:** 3-5 days

Optimize for "modest hardware" (4-8 GB RAM).

**Areas to Investigate:**
- **Index Caching** - Keep hot parts of index in memory
- **Query Caching** - Cache frequent searches
- **Batch Size Tuning** - Profile optimal batch size (currently 50)
- **Memory Profiling** - Identify memory leaks, optimize GC
- **Database Indexing** - Add indices on frequently queried columns
- **Lucene Configuration** - Tune segment size, merge policies

**Deliverables:**
- [ ] Establish performance baseline (query latency, memory usage)
- [ ] Implement query result caching (TTL: 5 min)
- [ ] Profile memory usage under load
- [ ] Optimize batch size based on profiling
- [ ] Add database indices (Status, Last_Modified)
- [ ] Document performance tuning guide

**Success Criteria:**
- Search latency < 200ms for 5,000 files
- Memory usage stable at < 50% of available RAM
- Zero memory leaks over 1 hour run

---

### 5.3 Testing & Quality
**Status:** 52/52 tests passing
**Dependencies:** All other features
**Time Estimate:** 5-7 days

Expand test coverage and add integration tests.

**Current Gaps:**
- No integration tests for full flow (index → search)
- No API endpoint tests
- No concurrency/stress tests
- Minimal unit test coverage

**Deliverables:**
- [ ] Expand unit test coverage to 70%+ for all services
- [ ] Add integration tests for end-to-end scenarios
- [ ] Add API endpoint tests (using MockMvc or similar)
- [ ] Add stress/load tests (100+ concurrent searches)
- [ ] Add edge case tests (empty index, special chars, large files)
- [ ] Set up CI/CD pipeline (GitHub Actions)

---

### 5.4 Configuration & Deployment
**Status:** Basic JSON config only
**Dependencies:** None
**Time Estimate:** 2-3 days

Make app easier to deploy and configure.

**Deliverables:**
- [ ] Support environment variables (override config.json)
- [ ] Support command-line arguments (--dir, --index-path, etc.)
- [ ] Add validation of config on startup (fail fast)
- [ ] Docker container (Dockerfile + build script)
- [ ] Systemd service file for Linux
- [ ] Windows batch/PowerShell startup scripts
- [ ] Documentation for common configurations

---

### 5.5 Logging & Monitoring
**Status:** System.out/err only
**Dependencies:** 5.1
**Time Estimate:** 2-3 days

Add structured logging and basic metrics.

**Deliverables:**
- [ ] Replace System.out/err with Log4j/SLF4j
- [ ] Configure log levels (DEBUG, INFO, WARN, ERROR)
- [ ] Add meaningful log messages (not just stack traces)
- [ ] Implement basic metrics (index size, file count, search count)
- [ ] Expose metrics via JMX or HTTP endpoint
- [ ] Add request/response logging for API

---

## Phase 6: Advanced Features (Nice-to-Have)

### 6.1 Advanced Query Features
- Fuzzy matching (approximate string matching)
- Wildcard queries (`java*`, `?ython`)
- Range queries (date ranges)
- Proximity search (terms within N words)
- Faceted search (drill-down by tag/category)

### 6.2 Content Processing
- Support more file types (PDF, DOCX extraction)
- Markdown-specific parsing (frontmatter, headings)
- Code syntax highlighting in snippets
- Support for PDF embedded metadata

### 6.3 User Features
- Search history/bookmarks
- Saved searches
- Custom ranking profiles
- Full-text indexing of linked files (transclusion)

### 6.4 Scaling
- Distributed indexing (shard by document hash)
- Incremental exports for backup
- Replication support
- Multi-user access control

---

## Dependency Graph

```
Phase 1: Foundation
├─ 1.1 REST Controller
├─ 1.2 Query Processing (depends: 1.1)
├─ 1.3 Search Results (depends: 1.2)
│
Phase 2: Enhancement
├─ 2.1 Snippets (depends: 1.3)
├─ 2.2 Ranking (depends: 1.3)
│
Phase 3: Web Dashboard (depends: Phase 1, 2)
├─ 3.1 React SPA Frontend
│
Phase 4: Metadata (parallel)
├─ 4.1 Metadata Extraction
├─ 4.2 Tag Filtering (depends: 4.1)
│
Phase 5: Polish
├─ 5.1 Error Handling
├─ 5.2 Performance (depends: 1.3)
├─ 5.3 Testing (depends: all features)
├─ 5.4 Config
├─ 5.5 Logging (depends: 5.1)
```

---

## Priority Recommendations

**For MVP (Minimum Viable Product):**
1. Phase 1: REST API + Query Processing (1-2 weeks) ✅
2. Phase 2: Snippets + Ranking (1 week) ✅
3. Phase 3: Web Dashboard UI (4-6 days) — NEXT

**For Beta (Usable Product):**
- Phase 4: Metadata + Tags (1 week)
- Phase 5.1: Error handling
- Phase 5.3: Testing & QA (1 week)

**For 1.0 (Polish):**
- Phase 5: All infrastructure
- Phase 6: Nice-to-have advanced features

---

## Known Technical Debt

1. **App.java FileWatcherService commented out** - Disabled due to concurrency issues
2. **No transaction handling** - Concurrent file operations may race
3. **Hardcoded file extensions** - `.txt` only, not configurable
4. **No input validation** - User queries could break parser
5. **Missing nullability checks** - Potential NPE in event handlers
6. **No connection pooling** - Single H2 connection, not thread-safe

---

## Quick Wins (Low Effort, High Value)

1. Fix typos and improve code comments (30 min)
2. Add proper logging instead of System.out (2-3 hours)
3. Add Maven shade plugin output to .gitignore (10 min)
4. Create Docker image (2-3 hours)

---

## Questions to Answer Before Development

1. **REST Framework:** Use Spring Boot (heavy but feature-rich) or Spark/Ktor (lightweight)?
2. **Concurrent File Changes:** How to handle simultaneous file modifications?
3. **Index Size:** Should we shard index for very large note collections?
4. **Multi-User:** Is this single-user or multi-user from the start?
5. **Mobile Support:** Is there a companion mobile app planned?
6. **Authentication:** Should search API require authentication?

---

**Last Updated:** 2026-03-05
**Version:** 2.0
