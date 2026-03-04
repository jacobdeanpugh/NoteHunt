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
- ❌ HTTP/REST API
- ❌ Query interface to Lucene index
- ❌ Result ranking and pagination
- ❌ Snippet extraction
- ❌ Metadata/tag support
- ❌ Error recovery
- ❌ Performance optimization

---

## Phase 1: Search API Foundation (High Priority)

### 1.1 Implement REST Controller
**Status:** Not started
**Dependencies:** None
**Time Estimate:** 3-5 days

Create a Spring Boot or similar REST framework integration to expose search endpoints.

**Required Endpoints:**
- `GET /search?q=<query>` - Basic text search
- `GET /search?q=<query>&limit=10&offset=0` - Pagination support
- `GET /health` - Service health check
- `GET /index/status` - Indexing progress/stats

**Deliverables:**
- [ ] Add REST framework to pom.xml (Spring Boot, Spark, or similar)
- [ ] Create SearchController class
- [ ] Implement `/search` endpoint with basic query parameter handling
- [ ] Add request validation and error responses
- [ ] Integrate with existing FileIndexer
- [ ] Write integration tests for endpoints

**Notes:**
- Consider lightweight frameworks (Spark, Ktor) given "modest hardware" requirement
- May need to refactor App.java to run HTTP server alongside file watcher

---

### 1.2 Query Processing Engine
**Status:** Not started
**Dependencies:** 1.1
**Time Estimate:** 2-3 days

Build logic to convert user queries into Lucene QueryParser queries.

**Required Features:**
- Parse simple text queries ("hello world")
- Support boolean operators (AND, OR, NOT)
- Quote phrases ("exact phrase search")
- Basic field targeting (title:, content:, etc.)
- Query validation and sanitization (prevent injection)

**Deliverables:**
- [ ] Create QueryParser utility class
- [ ] Implement queryString → Lucene Query conversion
- [ ] Add query validation
- [ ] Handle edge cases (empty queries, special characters)
- [ ] Write unit tests (10+ test cases)

**Notes:**
- Lucene has built-in QueryParser; wrap it for app-specific needs
- Should sanitize user input before passing to Lucene

---

### 1.3 Search Results Handler
**Status:** Not started
**Dependencies:** 1.2
**Time Estimate:** 3-4 days

Execute queries, return formatted results with metadata.

**Required Features:**
- Execute Lucene Query on IndexSearcher
- Return top N results (paginated)
- Include relevance score
- Include file path and metadata
- Support sorting (by relevance, date, filename)

**Deliverables:**
- [ ] Create SearchResult DTO (includes score, path, snippet preview, etc.)
- [ ] Implement query execution in FileIndexer
- [ ] Add pagination support (limit, offset/cursor)
- [ ] Add sorting strategies
- [ ] Write integration tests (search for actual indexed content)

---

## Phase 2: Result Enhancement (Medium Priority)

### 2.1 Snippet/Context Extraction
**Status:** Not started
**Dependencies:** 1.3
**Time Estimate:** 3-5 days

Extract relevant text excerpts showing matched terms in context.

**Required Features:**
- Highlight matched query terms in result text
- Extract surrounding context (e.g., 50 chars before/after match)
- Handle multiple matches per file
- Truncate long snippets gracefully

**Deliverables:**
- [ ] Create SnippetGenerator utility
- [ ] Implement term highlighting (wrapping matches in tags)
- [ ] Extract context windows around matches
- [ ] Handle edge cases (start of file, special characters, encoding)
- [ ] Add to SearchResult DTO
- [ ] Write unit tests

**Notes:**
- Lucene Highlighter library can assist but may be overkill
- Consider storing snippet in database for faster retrieval (cache)

---

### 2.2 Result Ranking & Relevance
**Status:** Not started
**Dependencies:** 1.3
**Time Estimate:** 2-3 days

Improve result ranking beyond basic Lucene scoring.

**Potential Strategies:**
- **Boost file modification recency** - Recently edited files ranked higher
- **Boost file size/richness** - Longer, more detailed files weighted higher
- **Personalization** - Future: track user clicks, boost clicked results
- **Metadata weighting** - Boost matches in title vs content

**Deliverables:**
- [ ] Study Lucene similarity/scoring
- [ ] Implement custom Similarity or Query wrapper
- [ ] Add database field for file importance/boost
- [ ] Integrate custom scoring into search results
- [ ] A/B test relevance improvements
- [ ] Write ranking tests

---

## Phase 3: Content Metadata & Filtering (Medium Priority)

### 3.1 Metadata Extraction
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

### 3.2 Tag/Category Filtering
**Status:** Not started
**Dependencies:** 3.1
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

## Phase 4: Infrastructure & Polish (Lower Priority)

### 4.1 Error Handling & Recovery
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
- **Network Issues** (future)

**Deliverables:**
- [ ] Add index validation on startup
- [ ] Implement index rebuild fallback
- [ ] Add retry logic for database operations
- [ ] Graceful error pages/responses for HTTP errors
- [ ] Comprehensive logging (Log4j, SLF4j)
- [ ] Add monitoring/observability (metrics, structured logs)

---

### 4.2 Performance Optimization
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

### 4.3 Testing & Quality
**Status:** Minimal (only 2 test classes)
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

### 4.4 Configuration & Deployment
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

### 4.5 Logging & Monitoring
**Status:** System.out/err only
**Dependencies:** 4.1
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

## Phase 5: Advanced Features (Nice-to-Have)

### 5.1 Advanced Query Features
- Fuzzy matching (approximate string matching)
- Wildcard queries (`java*`, `?ython`)
- Range queries (date ranges)
- Proximity search (terms within N words)
- Faceted search (drill-down by tag/category)

### 5.2 Content Processing
- Support more file types (PDF, DOCX extraction)
- Markdown-specific parsing (frontmatter, headings)
- Code syntax highlighting in snippets
- Support for PDF embedded metadata

### 5.3 User Features
- Search history/bookmarks
- Saved searches
- Custom ranking profiles
- Full-text indexing of linked files (transclusion)

### 5.4 Scaling
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
Phase 3: Metadata (parallel)
├─ 3.1 Metadata Extraction
├─ 3.2 Tag Filtering (depends: 3.1)
│
Phase 4: Polish
├─ 4.1 Error Handling
├─ 4.2 Performance (depends: 1.3)
├─ 4.3 Testing (depends: all features)
├─ 4.4 Config
├─ 4.5 Logging (depends: 4.1)
```

---

## Priority Recommendations

**For MVP (Minimum Viable Product):**
1. Phase 1: REST API + Query Processing (1-2 weeks)
2. Phase 1.3: Search Results (3-4 days)
3. Phase 4.1: Basic error handling

**For Beta (Usable Product):**
- Phase 2: Snippets + Ranking (1 week)
- Phase 3: Metadata + Tags (1 week)
- Phase 4.3: Testing & QA (1 week)

**For 1.0 (Polish):**
- Phase 4: All infrastructure
- Phase 5: Nice-to-have advanced features

---

## Known Technical Debt

1. **App.java is commented out** - FileWatcherService not running; only indexing from database
2. **No transaction handling** - Concurrent file operations may race
3. **Hardcoded file extensions** - `.txt` only, not configurable
4. **No input validation** - User queries could break parser
5. **Missing nullability checks** - Potential NPE in event handlers
6. **Typos in comments** - "databse" instead of "database"
7. **No connection pooling** - Single H2 connection, not thread-safe
8. **Status checking logic** - Some queries check against wrong status values

## Quick Wins (Low Effort, High Value)

1. Fix typos and improve code comments (30 min)
2. Add proper logging instead of System.out (2-3 hours)
3. Implement basic `/health` endpoint (1 hour)
4. Add Maven shade plugin output to .gitignore (10 min)
5. Create Docker image (2-3 hours)
6. Write ARCHITECTURE.md explaining component interactions (2 hours)

---

## Questions to Answer Before Development

1. **REST Framework:** Use Spring Boot (heavy but feature-rich) or Spark/Ktor (lightweight)?
2. **Concurrent File Changes:** How to handle simultaneous file modifications?
3. **Index Size:** Should we shard index for very large note collections?
4. **Multi-User:** Is this single-user or multi-user from the start?
5. **Mobile Support:** Is there a companion mobile app planned?
6. **Authentication:** Should search API require authentication?

---

**Last Updated:** 2024-01
**Version:** 1.0
