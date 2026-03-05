# Phase 1: REST API Design — Full Implementation

**Date:** 2026-03-04
**Status:** Approved
**Scope:** Phase 1.1 (REST Controller) + 1.2 (Query Parser) + 1.3 (Search Results Handler)
**Framework:** Spring Boot

---

## Overview

NoteHunt's REST API will expose full-text search capabilities via HTTP endpoints. Users can:
- Search indexed files with basic text queries
- Paginate results with limit/offset
- View health and indexing status
- Get rich results with snippet extraction and highlighting

This design covers the complete Phase 1 implementation end-to-end: HTTP layer → query parsing → Lucene execution → snippet extraction.

---

## Architecture

### Component Diagram

```
HTTP Request
    ↓
SearchController (validates params)
    ↓
QueryParser (string → Lucene Query)
    ↓
SearchResultHandler (executes, extracts snippets)
    ↓
FileIndexer (Lucene IndexSearcher)
    ↓
SearchResult DTOs (JSON response)
```

### Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| **Framework** | Spring Boot | Mature, excellent testing, built-in validation/error handling |
| **Query Type** | Basic text only | MVP scope; boolean/phrases deferred to Phase 2 |
| **Pagination** | Limit + Offset | Stateless, standard, simple to test |
| **Results** | Rich (with snippets) | Useful for future UI; snippet extraction in Phase 1 |

---

## API Specification

### 1. Search Endpoint

```
GET /search?q=<query>[&limit=10][&offset=0]
```

**Parameters:**
- `q` (required, string) — Search query (basic text, e.g., "python tutorial")
- `limit` (optional, int, default=10) — Results per page (1-100)
- `offset` (optional, int, default=0) — Pagination offset

**Response (200 OK):**
```json
{
  "results": [
    {
      "path": "/path/to/file.txt",
      "score": 0.95,
      "lastModified": "2024-01-15T10:30:00Z",
      "snippet": "Learn <match>python</match> programming basics. <match>Python</match> is widely used..."
    }
  ],
  "totalHits": 250,
  "limit": 10,
  "offset": 0,
  "timestamp": "2024-01-15T10:35:00Z"
}
```

**Error Responses:**
- **400 Bad Request** — Missing `q` param or invalid limit/offset
- **404 Not Found** — Index doesn't exist
- **500 Internal Server Error** — Lucene/database failure

---

### 2. Health Endpoint

```
GET /health
```

**Response (200 OK):**
```json
{
  "status": "UP",
  "indexing": false,
  "timestamp": "2024-01-15T10:35:00Z"
}
```

---

### 3. Index Status Endpoint

```
GET /index/status
```

**Response (200 OK):**
```json
{
  "filesIndexed": 1000,
  "pendingFiles": 50,
  "indexSize": "50MB",
  "lastUpdated": "2024-01-15T10:30:00Z",
  "timestamp": "2024-01-15T10:35:00Z"
}
```

---

## Data Models

### SearchResult DTO

```java
@Data
public class SearchResult {
    private String path;
    private double score;
    private LocalDateTime lastModified;
    private String snippet;  // HTML with <match> tags
}
```

### SearchResponse DTO

```java
@Data
public class SearchResponse {
    private List<SearchResult> results;
    private long totalHits;
    private int limit;
    private int offset;
    private LocalDateTime timestamp;
}
```

### ErrorResponse DTO

```java
@Data
public class ErrorResponse {
    private String error;
    private int status;
    private LocalDateTime timestamp;
}
```

---

## Implementation Components

### Phase 1.1: REST Controller (SearchController.java)

**Responsibilities:**
- Validate HTTP request parameters
- Call QueryParser to parse search string
- Call SearchResultHandler to execute query
- Return JSON responses with proper HTTP status codes

**Methods:**
- `search(String q, int limit, int offset)` → SearchResponse
- `health()` → HealthResponse
- `indexStatus()` → IndexStatusResponse

**Error Handling:**
- Catch IllegalArgumentException (invalid query) → 400
- Catch IOException (index missing) → 404
- Catch Exception (database/Lucene failure) → 500

---

### Phase 1.2: Query Parser (QueryParser.java)

**Responsibilities:**
- Convert user input string to Lucene Query objects
- Validate query syntax
- Handle edge cases (empty, special characters, etc.)

**Methods:**
- `parse(String queryString)` → org.apache.lucene.search.Query
- `validateQuery(String queryString)` → boolean

**Implementation Notes:**
- Use Lucene's `MultiFieldQueryParser` (searches "path" and "contents" fields)
- Default to AND operator between terms
- Escape special characters to prevent injection

---

### Phase 1.3: Search Result Handler (SearchResultHandler.java)

**Responsibilities:**
- Execute Lucene queries
- Extract snippets with context around matches
- Format results with relevance scores

**Methods:**
- `executeSearch(Query, int limit, int offset)` → SearchResponse
- `extractSnippet(Document, Query)` → String
- `highlightMatches(String text, Query)` → String (with <match> tags)

**Implementation Notes:**
- Use Lucene IndexSearcher from FileIndexer
- Extract 100-char context windows around matched terms
- Wrap matched terms in `<match>` tags for UI rendering

---

## Data Flow

### Search Request Flow

```
1. Client: GET /search?q=python&limit=10&offset=0
2. SearchController.search()
   ├─ Validate params (q required, limit 1-100, offset ≥ 0)
   ├─ QueryParser.parse("python")
   │  └─ Returns MultiFieldQueryParser.parse() result
   ├─ SearchResultHandler.executeSearch(query, 10, 0)
   │  ├─ IndexSearcher.search(query, limit + offset)
   │  ├─ For each hit:
   │  │  ├─ Create SearchResult(path, score)
   │  │  ├─ extractSnippet(doc, query)
   │  │  └─ highlightMatches(snippet)
   │  └─ Return SearchResponse with results + totalHits
   └─ Return JSON 200 OK
```

---

## Error Handling

### Exception Mappings

| Exception | HTTP Status | Response |
|-----------|-------------|----------|
| IllegalArgumentException (invalid query) | 400 | `{"error": "Invalid query syntax"}` |
| IOException (index missing) | 404 | `{"error": "Search index not found"}` |
| CorruptIndexException | 500 | `{"error": "Index corrupted, please rebuild"}` |
| SQLException (database) | 500 | `{"error": "Database error"}` |
| InterruptedException (timeout) | 504 | `{"error": "Search timeout"}` |

---

## Testing Strategy

### Unit Tests

1. **QueryParserTest** (10+ tests)
   - Valid queries: "python", "machine learning", single word
   - Invalid queries: empty string, null, special chars
   - Edge cases: leading/trailing spaces, accents

2. **SnippetExtractorTest** (8+ tests)
   - Extract context around matches
   - Truncate long snippets gracefully
   - Handle multiple matches in one document
   - Special characters in matches

3. **SearchResultHandlerTest** (6+ tests)
   - Execute query on mock index
   - Verify pagination (limit, offset)
   - Verify score ordering
   - Empty results

### Integration Tests

1. **SearchControllerTest** (10+ tests)
   - Valid search requests → 200 with results
   - Missing `q` param → 400
   - Invalid limit → 400
   - Health endpoint → 200
   - Index status → 200

2. **End-to-End Test** (2+ tests)
   - Index actual files
   - Search and verify results
   - Verify snippets contain matches

---

## Maven Dependencies

```xml
<!-- Spring Boot -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <version>3.2.0</version>
</dependency>

<!-- Lombok (for @Data, @Slf4j) -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>1.18.30</version>
    <scope>provided</scope>
</dependency>

<!-- Testing -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <version>3.2.0</version>
    <scope>test</scope>
</dependency>
```

---

## Known Limitations & Future Work

| Item | Phase | Notes |
|------|-------|-------|
| Boolean queries (AND, OR, NOT) | Phase 2 | Requires QueryParser enhancement |
| Field-specific search (title:, content:) | Phase 2 | Requires QueryParser enhancement |
| Phrase search ("exact match") | Phase 2 | Requires QueryParser enhancement |
| Snippet extraction | Phase 1 | Included in this design |
| Result ranking beyond Lucene scores | Phase 2 | Custom Similarity layer |
| Tag filtering | Phase 3 | Requires metadata extraction |
| Authentication/authorization | Future | Not needed for MVP |

---

## Success Criteria

- [x] 3 endpoints implemented and tested
- [x] All 10+ unit tests passing
- [x] All integration tests passing
- [x] Snippets extracted and highlighted correctly
- [x] Pagination works (limit + offset)
- [x] Error responses properly formatted
- [x] Spring Boot starts with embedded Lucene index

---

**Approved by:** User
**Next Step:** Invoke `superpowers:writing-plans` to create detailed implementation plan
