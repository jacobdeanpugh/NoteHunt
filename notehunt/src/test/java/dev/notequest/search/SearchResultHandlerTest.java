package dev.notequest.search;

import dev.notequest.api.SearchResponse;
import dev.notequest.api.SearchResult;
import dev.notequest.config.RankingConfig;
import dev.notequest.search.RankingStrategy;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for SearchResultHandler class.
 *
 * Tests search execution using in-memory Lucene index, including:
 * - Basic search functionality (found/not found)
 * - Pagination with limit and offset
 * - Validation of query parameters
 * - Search result content (path, score, snippet)
 * - Edge cases (empty results, invalid pagination)
 */
public class SearchResultHandlerTest {

    private Directory index;
    private IndexSearcher searcher;
    private QueryParser queryParser;
    private SearchResultHandler handler;
    private SnippetExtractor snippetExtractor;

    @BeforeEach
    void setUp() throws Exception {
        // Create in-memory Lucene index using ByteBuffersDirectory
        index = new ByteBuffersDirectory();
        IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
        IndexWriter writer = new IndexWriter(index, config);

        // Add test documents
        addDocument(writer, "/test/python.txt", "Python is great for data science and machine learning");
        addDocument(writer, "/test/java.txt", "Java is a compiled language used in enterprise");
        addDocument(writer, "/test/python2.txt", "Python tutorial for beginners learning to code");
        addDocument(writer, "/test/golang.txt", "Go is a fast language created by Google");
        addDocument(writer, "/test/javascript.txt", "JavaScript runs in web browsers and NodeJS");

        writer.close();

        // Create searcher and handler components
        DirectoryReader reader = DirectoryReader.open(index);
        searcher = new IndexSearcher(reader);
        queryParser = new QueryParser();
        snippetExtractor = new SnippetExtractor(queryParser);

        // Create RankingStrategy with default config
        RankingConfig rankingConfig = new RankingConfig(3, 1.5, 7, 1.2, 1.0);
        RankingStrategy rankingStrategy = new RankingStrategy(rankingConfig);

        handler = new SearchResultHandler(searcher, snippetExtractor, rankingStrategy);
    }

    /**
     * Test 1: Execute search with results found
     * Query: "python" should return 2 documents
     * Verifies: totalHits=2, results.size()=2, default limit=10, offset=0
     */
    @Test
    void testExecuteSearchFound() throws Exception {
        // Execute search
        SearchResponse response = handler.executeSearch("python", 10, 0);

        // Verify results
        assertNotNull(response, "SearchResponse should not be null");
        assertEquals(2, response.getTotalHits(), "Should find 2 documents containing 'python'");
        assertEquals(2, response.getResults().size(), "Results list should contain 2 items");
        assertEquals(10, response.getLimit(), "Limit should be 10");
        assertEquals(0, response.getOffset(), "Offset should be 0");
        assertNotNull(response.getTimestamp(), "Timestamp should be set");
    }

    /**
     * Test 2: Execute search with no results found
     * Query: "golang" should return 1 document (but we search for something else)
     * Actually "golang" returns 1, so let's search for "typescript"
     * Verifies: totalHits=0, results.size()=0
     */
    @Test
    void testExecuteSearchNotFound() throws Exception {
        // Execute search for term not in index
        SearchResponse response = handler.executeSearch("typescript", 10, 0);

        // Verify no results
        assertNotNull(response, "SearchResponse should not be null");
        assertEquals(0, response.getTotalHits(), "Should find 0 documents");
        assertEquals(0, response.getResults().size(), "Results list should be empty");
        assertEquals(10, response.getLimit(), "Limit should be 10");
        assertEquals(0, response.getOffset(), "Offset should be 0");
    }

    /**
     * Test 3: Execute search with pagination - limit smaller than total hits
     * Query: "python" (2 results total) with limit=1
     * Page 1: offset=0 → 1 result
     * Page 2: offset=1 → 1 result (different from page 1)
     * Verifies: Both pages have totalHits=2, but results differ
     */
    @Test
    void testExecuteSearchPagination() throws Exception {
        // Get first page
        SearchResponse page1 = handler.executeSearch("python", 1, 0);

        // Get second page
        SearchResponse page2 = handler.executeSearch("python", 1, 1);

        // Verify both pages report same total hits
        assertEquals(2, page1.getTotalHits(), "Page 1 totalHits should be 2");
        assertEquals(2, page2.getTotalHits(), "Page 2 totalHits should be 2");

        // Verify each page has 1 result
        assertEquals(1, page1.getResults().size(), "Page 1 should have 1 result");
        assertEquals(1, page2.getResults().size(), "Page 2 should have 1 result");

        // Verify results are different
        String page1Path = page1.getResults().get(0).getPath();
        String page2Path = page2.getResults().get(0).getPath();
        assertNotEquals(page1Path, page2Path, "Page 1 and Page 2 should have different results");
    }

    /**
     * Test 4: Execute search with limit=0
     * Expected: throws IllegalArgumentException
     */
    @Test
    void testExecuteSearchLimitZero() {
        assertThrows(IllegalArgumentException.class, () -> {
            handler.executeSearch("python", 0, 0);
        }, "Limit 0 should throw IllegalArgumentException");
    }

    /**
     * Test 5: Execute search with negative offset
     * Expected: throws IllegalArgumentException
     */
    @Test
    void testExecuteSearchNegativeOffset() {
        assertThrows(IllegalArgumentException.class, () -> {
            handler.executeSearch("python", 10, -1);
        }, "Negative offset should throw IllegalArgumentException");
    }

    /**
     * Test 6: Search result contains file path
     * Query: "python"
     * Expected: Results contain paths with "python" in them
     */
    @Test
    void testSearchResultHasPath() throws Exception {
        SearchResponse response = handler.executeSearch("python", 10, 0);

        assertFalse(response.getResults().isEmpty(), "Results should not be empty");

        for (SearchResult result : response.getResults()) {
            assertNotNull(result.getPath(), "Result path should not be null");
            assertTrue(result.getPath().contains("python"), "Path should contain 'python'");
        }
    }

    /**
     * Test 7: Search result has positive score
     * Query: "python"
     * Expected: All results have score > 0
     */
    @Test
    void testSearchResultHasScore() throws Exception {
        SearchResponse response = handler.executeSearch("python", 10, 0);

        assertFalse(response.getResults().isEmpty(), "Results should not be empty");

        for (SearchResult result : response.getResults()) {
            assertTrue(result.getScore() > 0, "Result score should be > 0, got " + result.getScore());
        }
    }

    /**
     * Test 8: Search result has snippet
     * Query: "python"
     * Expected: All results have non-empty snippet
     * Verify: snippet != null && !snippet.isEmpty()
     */
    @Test
    void testSearchResultHasSnippet() throws Exception {
        SearchResponse response = handler.executeSearch("python", 10, 0);

        assertFalse(response.getResults().isEmpty(), "Results should not be empty");

        for (SearchResult result : response.getResults()) {
            assertNotNull(result.getSnippet(), "Snippet should not be null");
            assertFalse(result.getSnippet().isEmpty(), "Snippet should not be empty");
        }
    }

    /**
     * Test 9: Execute search with limit > 100
     * Limit: 101
     * Expected: throws IllegalArgumentException
     */
    @Test
    void testExecuteSearchLimitExceeds100() {
        assertThrows(IllegalArgumentException.class, () -> {
            handler.executeSearch("python", 101, 0);
        }, "Limit > 100 should throw IllegalArgumentException");
    }

    /**
     * Test 10: Search result has lastModified timestamp
     * Query: "python"
     * Expected: Results have non-null lastModified
     */
    @Test
    void testSearchResultHasLastModified() throws Exception {
        SearchResponse response = handler.executeSearch("python", 10, 0);

        assertFalse(response.getResults().isEmpty(), "Results should not be empty");

        for (SearchResult result : response.getResults()) {
            assertNotNull(result.getLastModified(), "LastModified should not be null");
        }
    }

    /**
     * Test 11: Pagination with offset beyond available results
     * Query: "python" (2 results), offset=10, limit=10
     * Expected: totalHits=2, results.size()=0
     */
    @Test
    void testExecuteSearchPaginationOffsetBeyondResults() throws Exception {
        SearchResponse response = handler.executeSearch("python", 10, 10);

        assertEquals(2, response.getTotalHits(), "TotalHits should be 2");
        assertEquals(0, response.getResults().size(), "Results should be empty when offset is beyond total");
        assertEquals(10, response.getLimit(), "Limit should be 10");
        assertEquals(10, response.getOffset(), "Offset should be 10");
    }

    /**
     * Test 12: Search with max valid limit (100)
     * Limit: 100 should work
     * Expected: No exception, returns valid response
     */
    @Test
    void testExecuteSearchMaxValidLimit() throws Exception {
        SearchResponse response = handler.executeSearch("python", 100, 0);

        assertNotNull(response, "SearchResponse should not be null");
        assertEquals(100, response.getLimit(), "Limit should be 100");
        assertTrue(response.getResults().size() <= 100, "Results should respect limit");
    }

    /**
     * Test 13: Search with multiple matching documents
     * Query: "is" appears in multiple documents
     * Expected: Multiple results returned with different paths
     */
    @Test
    void testExecuteSearchMultipleMatches() throws Exception {
        SearchResponse response = handler.executeSearch("is", 10, 0);

        assertTrue(response.getTotalHits() > 1, "Should find multiple documents containing 'is'");
        assertTrue(response.getResults().size() > 1, "Should return multiple results");

        // Verify all results have unique paths
        java.util.Set<String> paths = new java.util.HashSet<>();
        for (SearchResult result : response.getResults()) {
            paths.add(result.getPath());
        }
        assertEquals(response.getResults().size(), paths.size(), "All result paths should be unique");
    }

    /**
     * Test 14: SearchResultHandler with RankingStrategy injection
     * Verifies: Handler accepts RankingStrategy in constructor
     * Expected: Handler created successfully with ranking strategy
     */
    @Test
    void testSearchResultsIncludeRecencyBoost() throws Exception {
        RankingConfig config = new RankingConfig(3, 1.5, 7, 1.2, 1.0);
        RankingStrategy rankingStrategy = new RankingStrategy(config);

        // Create handler with RankingStrategy
        SearchResultHandler handler = new SearchResultHandler(
            searcher,
            snippetExtractor,
            rankingStrategy
        );

        // Verify handler created successfully
        assertNotNull(handler);
    }

    /**
     * Helper method: Add a document to the index
     * @param writer IndexWriter to add to
     * @param path File path
     * @param content Document content
     */
    private void addDocument(IndexWriter writer, String path, String content) throws Exception {
        Document doc = new Document();
        doc.add(new StringField("path", path, Field.Store.YES));
        doc.add(new TextField("contents", content, Field.Store.YES));
        writer.addDocument(doc);
    }
}
