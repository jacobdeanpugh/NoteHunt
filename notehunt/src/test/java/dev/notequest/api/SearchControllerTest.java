package dev.notequest.api;

import dev.notequest.search.SearchResultHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * REST API tests for SearchController.
 *
 * Tests the HTTP endpoints using MockMvc with mocked SearchResultHandler:
 * - GET /search - search endpoint with query, limit, offset
 * - GET /health - health check endpoint
 * - GET /index/status - index status endpoint
 *
 * Tests cover:
 * - Valid search queries with pagination
 * - Missing/invalid query parameters
 * - Default pagination values
 * - Special characters in query strings
 * - Response structure and timestamp presence
 * - Health and index status endpoints
 */
@WebMvcTest(SearchController.class)
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchResultHandler searchResultHandler;

    private SearchResponse mockSearchResponse;
    private SearchResult testResult1;
    private SearchResult testResult2;

    @BeforeEach
    void setUp() {
        // Initialize test data
        testResult1 = SearchResult.builder()
                .path("/test/file.txt")
                .score(1.5f)
                .lastModified(LocalDateTime.of(2025, 3, 1, 12, 0, 0))
                .snippet("Python is a great programming language for data science")
                .build();

        testResult2 = SearchResult.builder()
                .path("/test/python_guide.txt")
                .score(1.2f)
                .lastModified(LocalDateTime.of(2025, 3, 2, 10, 30, 0))
                .snippet("Python tutorial covers basics and advanced topics")
                .build();

        mockSearchResponse = SearchResponse.builder()
                .results(Arrays.asList(testResult1, testResult2))
                .totalHits(2)
                .limit(10)
                .offset(0)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Test 1: testSearchWithValidQuery
     * Endpoint: GET /search?q=python&limit=10&offset=0
     * Expected: 200 OK with JSON response containing search results
     * Verifies: status isOk, results hasSize(1), totalHits is(1)
     */
    @Test
    void testSearchWithValidQuery() throws Exception {
        // Mock response with 1 result
        SearchResponse singleResultResponse = SearchResponse.builder()
                .results(Arrays.asList(testResult1))
                .totalHits(1)
                .limit(10)
                .offset(0)
                .timestamp(LocalDateTime.now())
                .build();

        when(searchResultHandler.executeSearch("python", 10, 0))
                .thenReturn(singleResultResponse);

        mockMvc.perform(get("/search")
                .param("q", "python")
                .param("limit", "10")
                .param("offset", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results", hasSize(1)))
                .andExpect(jsonPath("$.totalHits", is(1)))
                .andExpect(jsonPath("$.results[0].path", containsString("file.txt")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));

        verify(searchResultHandler).executeSearch("python", 10, 0);
    }

    /**
     * Test 2: testSearchMissingQuery
     * Endpoint: GET /search (no q param)
     * Expected: 400 Bad Request
     * Verifies: status isBadRequest
     */
    @Test
    void testSearchMissingQuery() throws Exception {
        mockMvc.perform(get("/search"))
                .andExpect(status().isBadRequest());

        verify(searchResultHandler, never()).executeSearch(anyString(), anyInt(), anyInt());
    }

    /**
     * Test 3: testSearchInvalidLimit
     * Endpoint: GET /search?q=python&limit=0
     * Expected: 400 Bad Request
     * Verifies: status isBadRequest
     */
    @Test
    void testSearchInvalidLimit() throws Exception {
        mockMvc.perform(get("/search")
                .param("q", "python")
                .param("limit", "0"))
                .andExpect(status().isBadRequest());

        verify(searchResultHandler, never()).executeSearch(anyString(), anyInt(), anyInt());
    }

    /**
     * Test 4: testSearchInvalidOffset
     * Endpoint: GET /search?q=python&offset=-1
     * Expected: 400 Bad Request
     * Verifies: status isBadRequest
     */
    @Test
    void testSearchInvalidOffset() throws Exception {
        mockMvc.perform(get("/search")
                .param("q", "python")
                .param("offset", "-1"))
                .andExpect(status().isBadRequest());

        verify(searchResultHandler, never()).executeSearch(anyString(), anyInt(), anyInt());
    }

    /**
     * Test 5: testSearchDefaultPagination
     * Endpoint: GET /search?q=python (no limit/offset)
     * Expected: Uses default limit=10, offset=0
     * Verifies: Request is processed with default values
     */
    @Test
    void testSearchDefaultPagination() throws Exception {
        when(searchResultHandler.executeSearch("python", 10, 0))
                .thenReturn(mockSearchResponse);

        mockMvc.perform(get("/search")
                .param("q", "python"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limit", is(10)))
                .andExpect(jsonPath("$.offset", is(0)));

        verify(searchResultHandler).executeSearch("python", 10, 0);
    }

    /**
     * Test 6: testSearchWithPagination
     * Endpoint: GET /search?q=python&limit=5&offset=10
     * Expected: 200 OK with specified pagination
     * Verifies: limit is(5), offset is(10)
     */
    @Test
    void testSearchWithPagination() throws Exception {
        SearchResponse paginatedResponse = SearchResponse.builder()
                .results(Arrays.asList(testResult1))
                .totalHits(50)
                .limit(5)
                .offset(10)
                .timestamp(LocalDateTime.now())
                .build();

        when(searchResultHandler.executeSearch("python", 5, 10))
                .thenReturn(paginatedResponse);

        mockMvc.perform(get("/search")
                .param("q", "python")
                .param("limit", "5")
                .param("offset", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limit", is(5)))
                .andExpect(jsonPath("$.offset", is(10)))
                .andExpect(jsonPath("$.totalHits", is(50)));

        verify(searchResultHandler).executeSearch("python", 5, 10);
    }

    /**
     * Test 7: testHealthEndpoint
     * Endpoint: GET /health
     * Expected: 200 OK with status "UP"
     * Verifies: status isOk, body contains "UP"
     */
    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    /**
     * Test 8: testIndexStatusEndpoint
     * Endpoint: GET /index/status
     * Expected: 200 OK with numeric fields
     * Verifies: status isOk, response contains index metrics
     */
    @Test
    void testIndexStatusEndpoint() throws Exception {
        mockMvc.perform(get("/index/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filesIndexed", isA(Number.class)))
                .andExpect(jsonPath("$.pendingFiles", isA(Number.class)))
                .andExpect(jsonPath("$.indexSize", notNullValue()))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    /**
     * Test 9: testSearchWithSpecialCharacters
     * Endpoint: GET /search?q=python%2Fjava
     * Expected: 200 OK with query properly decoded
     * Verifies: Special characters are handled correctly
     */
    @Test
    void testSearchWithSpecialCharacters() throws Exception {
        when(searchResultHandler.executeSearch("python/java", 10, 0))
                .thenReturn(SearchResponse.builder()
                        .results(Collections.emptyList())
                        .totalHits(0)
                        .limit(10)
                        .offset(0)
                        .timestamp(LocalDateTime.now())
                        .build());

        mockMvc.perform(get("/search")
                .param("q", "python/java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHits", is(0)));

        verify(searchResultHandler).executeSearch("python/java", 10, 0);
    }

    /**
     * Test 10: testSearchResponseHasTimestamp
     * Verify: All responses include timestamp field
     */
    @Test
    void testSearchResponseHasTimestamp() throws Exception {
        when(searchResultHandler.executeSearch("python", 10, 0))
                .thenReturn(mockSearchResponse);

        mockMvc.perform(get("/search")
                .param("q", "python"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.timestamp", matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")));
    }

    /**
     * Test 11: testSearchWithLimitExceedsMax
     * Endpoint: GET /search?q=python&limit=101
     * Expected: 400 Bad Request
     * Verifies: Limit validation (max 100)
     */
    @Test
    void testSearchWithLimitExceedsMax() throws Exception {
        mockMvc.perform(get("/search")
                .param("q", "python")
                .param("limit", "101"))
                .andExpect(status().isBadRequest());

        verify(searchResultHandler, never()).executeSearch(anyString(), anyInt(), anyInt());
    }

    /**
     * Test 12: testSearchWithEmptyResults
     * Endpoint: GET /search?q=nonexistentterm
     * Expected: 200 OK with empty results list
     * Verifies: totalHits is(0), results hasSize(0)
     */
    @Test
    void testSearchWithEmptyResults() throws Exception {
        SearchResponse emptyResponse = SearchResponse.builder()
                .results(Collections.emptyList())
                .totalHits(0)
                .limit(10)
                .offset(0)
                .timestamp(LocalDateTime.now())
                .build();

        when(searchResultHandler.executeSearch("nonexistentterm", 10, 0))
                .thenReturn(emptyResponse);

        mockMvc.perform(get("/search")
                .param("q", "nonexistentterm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHits", is(0)))
                .andExpect(jsonPath("$.results", hasSize(0)));
    }

    /**
     * Test 13: testSearchResultsHaveRequiredFields
     * Endpoint: GET /search?q=python
     * Expected: Each result has path, score, snippet, lastModified
     * Verifies: Response structure completeness
     */
    @Test
    void testSearchResultsHaveRequiredFields() throws Exception {
        when(searchResultHandler.executeSearch("python", 10, 0))
                .thenReturn(mockSearchResponse);

        mockMvc.perform(get("/search")
                .param("q", "python"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].path", notNullValue()))
                .andExpect(jsonPath("$.results[0].score", isA(Number.class)))
                .andExpect(jsonPath("$.results[0].snippet", notNullValue()))
                .andExpect(jsonPath("$.results[0].lastModified", notNullValue()));
    }

    /**
     * Test 14: testSearchWithMaxValidLimit
     * Endpoint: GET /search?q=python&limit=100
     * Expected: 200 OK (limit=100 is valid max)
     * Verifies: Boundary condition - limit exactly at max
     */
    @Test
    void testSearchWithMaxValidLimit() throws Exception {
        SearchResponse maxLimitResponse = SearchResponse.builder()
                .results(Arrays.asList(testResult1, testResult2))
                .totalHits(2)
                .limit(100)
                .offset(0)
                .timestamp(LocalDateTime.now())
                .build();

        when(searchResultHandler.executeSearch("python", 100, 0))
                .thenReturn(maxLimitResponse);

        mockMvc.perform(get("/search")
                .param("q", "python")
                .param("limit", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limit", is(100)));

        verify(searchResultHandler).executeSearch("python", 100, 0);
    }

    /**
     * Test 15: testSearchWithMinValidLimit
     * Endpoint: GET /search?q=python&limit=1
     * Expected: 200 OK (limit=1 is valid min)
     * Verifies: Boundary condition - limit exactly at min
     */
    @Test
    void testSearchWithMinValidLimit() throws Exception {
        SearchResponse minLimitResponse = SearchResponse.builder()
                .results(Arrays.asList(testResult1))
                .totalHits(2)
                .limit(1)
                .offset(0)
                .timestamp(LocalDateTime.now())
                .build();

        when(searchResultHandler.executeSearch("python", 1, 0))
                .thenReturn(minLimitResponse);

        mockMvc.perform(get("/search")
                .param("q", "python")
                .param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limit", is(1)))
                .andExpect(jsonPath("$.results", hasSize(1)));

        verify(searchResultHandler).executeSearch("python", 1, 0);
    }
}
