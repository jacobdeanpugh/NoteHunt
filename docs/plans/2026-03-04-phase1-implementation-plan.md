# Phase 1 REST API Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** Implement a complete Phase 1 REST API (SearchController, QueryParser, SearchResultHandler) with TDD incremental testing.

**Architecture:** Spring Boot wraps existing FileIndexer. HTTP layer → QueryParser → SearchResultHandler → Lucene execution → snippet extraction → JSON response.

**Tech Stack:** Spring Boot 3.2, Lucene 10.3.1, Lombok, JUnit 5, Mockito

---

## Task List (18 Tasks)

### Phase 0: Setup & Dependencies

#### Task 1: Add Spring Boot & Testing Dependencies to pom.xml
- Add Spring Boot starter-web, starter-test, and Lombok
- Update Maven build plugins
- Verify clean build

#### Task 2-6: Create DTOs (5 Tasks)
- SearchResult.java
- SearchResponse.java
- ErrorResponse.java
- HealthResponse.java
- IndexStatusResponse.java

### Phase 1: QueryParser (Phase 1.2)

#### Task 7: Write QueryParser Tests (10+ test cases)
- Simple word parsing
- Multiple word queries
- Invalid input handling
- Edge cases (spaces, accents, special chars)

#### Task 8: Implement QueryParser
- Use Lucene MultiFieldQueryParser
- Validate queries
- Handle parse exceptions

### Phase 2: Snippet Extraction (Phase 1.3 support)

#### Task 9: Write SnippetExtractor Tests (8+ test cases)
- Extract snippets with context
- Highlight matched terms
- Truncate long text
- Handle no matches

#### Task 10: Implement SnippetExtractor
- Use Lucene Highlighter
- Extract context windows
- Truncate with ellipsis
- Generate <match> tags

### Phase 3: SearchResultHandler (Phase 1.3)

#### Task 11: Write SearchResultHandler Tests (9+ test cases)
- Execute queries on mock index
- Test pagination (limit/offset)
- Verify score calculation
- Handle empty results

#### Task 12: Implement SearchResultHandler
- Execute Lucene queries
- Build SearchResult objects
- Implement pagination
- Extract snippets for each result

### Phase 4: REST Controller (Phase 1.1)

#### Task 13: Create Spring Boot Application Main Class
- Convert App.java to @SpringBootApplication
- Ensure startup works

#### Task 14: Create Spring Boot Configuration Class
- Wire SearchConfiguration with beans
- Provide SearchResultHandler bean

#### Task 15: Write SearchController Tests (9+ test cases)
- Valid search requests
- Missing/invalid parameters
- Pagination validation
- Health and status endpoints

#### Task 16: Implement SearchController
- /search endpoint with validation
- /health endpoint
- /index/status endpoint
- Error response handling

### Phase 5: Integration Testing

#### Task 17: Write End-to-End Integration Test (6+ test cases)
- Full search flow with real index
- Pagination behavior
- Snippet extraction
- Multi-word search

#### Task 18: Run All Tests & Verify Build
- Run full test suite
- Build JAR
- Document test results

---

## Task Execution Order

**Serial (some dependencies):**
1. Task 1 (dependencies) → 2-6 (DTOs - independent) → 7-8 (QueryParser) → 9-10 (SnippetExtractor) → 11-12 (SearchResultHandler) → 13-16 (Controller) → 17-18 (Integration)

**Parallel opportunity:**
- Tasks 2-6 can run in parallel (independent DTOs)
- But we'll execute serially for simplicity

---

## Test Summary

| Component | Tests | Status |
|-----------|-------|--------|
| QueryParser | 10 | Pending |
| SnippetExtractor | 8 | Pending |
| SearchResultHandler | 9 | Pending |
| SearchController | 9 | Pending |
| Integration | 6 | Pending |
| **Total** | **42** | **Pending** |

---

## Success Criteria

✅ All 42 tests passing
✅ 3 REST endpoints implemented
✅ Spring Boot builds successfully
✅ Query parsing working
✅ Snippet extraction with highlighting
✅ Pagination functional
✅ Error handling in place
✅ All code committed with meaningful messages
