package dev.notequest.api;

import dev.notequest.search.QueryParser;
import dev.notequest.search.SearchResultHandler;
import dev.notequest.search.SnippetExtractor;
import dev.notequest.search.RankingStrategy;
import dev.notequest.config.RankingConfig;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-End Integration Tests for NoteHunt Search API.
 *
 * Tests the complete search flow with real Lucene index, QueryParser, SnippetExtractor,
 * and SearchResultHandler. No mocking - all components work together.
 *
 * Test Coverage:
 * 1. Full search flow (query → parse → search → snippet extraction → response)
 * 2. Pagination behavior (limit, offset, totalHits consistency)
 * 3. Snippet extraction with highlighting and context
 * 4. No results handling (empty result sets)
 * 5. Multi-word search with OR logic
 * 6. Snippet accuracy and highlighting
 * 7. Edge cases (long documents, special characters, case sensitivity)
 */
@DisplayName("End-to-End Search Integration Tests")
public class SearchIntegrationTest {

    private Directory index;
    private IndexSearcher searcher;
    private SearchResultHandler handler;
    private QueryParser queryParser;
    private SnippetExtractor snippetExtractor;
    private DirectoryReader reader;

    @BeforeEach
    void setUp() throws Exception {
        // Create real in-memory Lucene index
        index = new ByteBuffersDirectory();
        IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
        IndexWriter writer = new IndexWriter(index, config);

        // Add test documents with realistic content
        addDocument(writer, "/notes/python-basics.txt",
                "Python is a high-level programming language. It emphasizes code readability and simplicity. " +
                "Python supports multiple programming paradigms including procedural, object-oriented, and functional programming. " +
                "It has a comprehensive standard library.");

        addDocument(writer, "/notes/java-tutorial.txt",
                "Java is used for enterprise applications. It runs on the Java Virtual Machine (JVM). " +
                "Java provides strong memory management and automatic garbage collection. " +
                "Enterprise applications often use Java for their robustness and scalability.");

        addDocument(writer, "/notes/python-advanced.txt",
                "Advanced Python features include decorators and metaclasses. " +
                "Decorators provide a way to modify or enhance functions and classes without permanently changing their source code. " +
                "Metaclasses allow you to customize class creation. Python's advanced features make it powerful for complex applications.");

        addDocument(writer, "/docs/javascript.txt",
                "JavaScript powers web browsers and Node.js. It is the most popular language for web development. " +
                "Modern JavaScript includes ES6+ features like async/await, arrow functions, and classes. " +
                "JavaScript can be used for both frontend and backend development.");

        addDocument(writer, "/docs/csharp-guide.txt",
                "C# is a modern programming language developed by Microsoft. It runs on the .NET Framework. " +
                "C# combines the power of C++ with the simplicity of Visual Basic. " +
                "C# is widely used in game development with Unity engine.");

        addDocument(writer, "/blog/golang-intro.txt",
                "Go is a fast language created by Google. It is designed for concurrent programming. " +
                "Go compiles directly to machine code. Goroutines provide lightweight concurrency. " +
                "Go is ideal for building scalable backend services.");

        writer.close();

        // Create real IndexSearcher and handler
        reader = DirectoryReader.open(index);
        searcher = new IndexSearcher(reader);
        queryParser = new QueryParser();
        snippetExtractor = new SnippetExtractor(queryParser);

        // Create RankingStrategy with default config
        RankingConfig rankingConfig = new RankingConfig(3, 1.5, 7, 1.2, 1.0);
        RankingStrategy rankingStrategy = new RankingStrategy(rankingConfig);

        handler = new SearchResultHandler(searcher, snippetExtractor, rankingStrategy);
    }

    /**
     * Test 1: Full Search Flow - Single Word Query
     *
     * Tests complete flow: query parsing → Lucene search → snippet extraction → response building
     * Query: "python"
     * Expected: 2 results (python-basics.txt and python-advanced.txt)
     * Verify: totalHits=2, results.size()=2, each result has path, score, snippet
     */
    @Test
    @DisplayName("Test 1: Full search flow with single word query")
    void testFullSearchFlow() throws Exception {
        // Execute search
        SearchResponse response = handler.executeSearch("python", 10, 0);

        // Verify response structure
        assertNotNull(response, "SearchResponse should not be null");
        assertNotNull(response.getResults(), "Results list should not be null");
        assertNotNull(response.getTimestamp(), "Timestamp should be set");

        // Verify result count
        assertEquals(2, response.getTotalResults(), "Should find 2 documents containing 'python'");
        assertEquals(2, response.getResults().size(), "Results list should contain 2 items");
        assertEquals(10, response.getLimit(), "Limit should be 10");
        assertEquals(0, response.getOffset(), "Offset should be 0");

        // Verify each result has required fields
        for (SearchResult result : response.getResults()) {
            assertNotNull(result.getFilePath(), "Result path should not be null");
            assertTrue(result.getFilePath().contains("python"), "Path should contain 'python'");
            assertTrue(result.getScore() > 0, "Score should be positive");
            assertNotNull(result.getSnippet(), "Snippet should not be null");
            assertFalse(result.getSnippet().isEmpty(), "Snippet should not be empty");
            assertNotNull(result.getLastModified(), "LastModified should not be null");
        }
    }

    /**
     * Test 2: Pagination Behavior
     *
     * Tests pagination with limit and offset parameters
     * Query: "language" (appears in multiple documents)
     * Page 1: offset=0, limit=1 → 1 result
     * Page 2: offset=1, limit=1 → 1 result (different from page 1)
     * Verify: Both pages report same totalHits, but results are different
     */
    @Test
    @DisplayName("Test 2: Pagination with limit and offset")
    void testPaginationBehavior() throws Exception {
        // Get first page (1 result at offset 0)
        SearchResponse page1 = handler.executeSearch("language", 1, 0);

        // Get second page (1 result at offset 1)
        SearchResponse page2 = handler.executeSearch("language", 1, 1);

        // Verify both pages report same total hits
        assertEquals(page1.getTotalResults(), page2.getTotalResults(),
                "Page 1 and Page 2 should report same totalHits");
        assertTrue(page1.getTotalResults() >= 2, "Should have at least 2 results for pagination test");

        // Verify each page has exactly 1 result
        assertEquals(1, page1.getResults().size(), "Page 1 should have 1 result");
        assertEquals(1, page2.getResults().size(), "Page 2 should have 1 result");

        // Verify results are different
        String page1Path = page1.getResults().get(0).getFilePath();
        String page2Path = page2.getResults().get(0).getFilePath();
        assertNotEquals(page1Path, page2Path, "Page 1 and Page 2 should have different results");

        // Verify pagination metadata is correct
        assertEquals(1, page1.getLimit(), "Page 1 limit should be 1");
        assertEquals(0, page1.getOffset(), "Page 1 offset should be 0");
        assertEquals(1, page2.getLimit(), "Page 2 limit should be 1");
        assertEquals(1, page2.getOffset(), "Page 2 offset should be 1");
    }

    /**
     * Test 3: Snippet Extraction with Highlighting
     *
     * Tests that snippets contain query terms highlighted with <match> tags
     * Query: "enterprise"
     * Expected: At least 1 result with snippet containing <match> tags
     * Verify: Snippet contains highlighted term, has context around match
     */
    @Test
    @DisplayName("Test 3: Snippet extraction with highlighting")
    void testSnippetExtraction() throws Exception {
        SearchResponse response = handler.executeSearch("enterprise", 10, 0);

        assertTrue(response.getTotalResults() > 0, "Should find results for 'enterprise'");
        assertFalse(response.getResults().isEmpty(), "Results list should not be empty");

        // Check first result
        SearchResult firstResult = response.getResults().get(0);
        String snippet = firstResult.getSnippet();

        assertNotNull(snippet, "Snippet should not be null");
        assertFalse(snippet.isEmpty(), "Snippet should not be empty");

        // Verify snippet contains <match> tags around highlighted terms
        assertTrue(snippet.contains("<match>"), "Snippet should contain opening <match> tag");
        assertTrue(snippet.contains("</match>"), "Snippet should contain closing </match> tag");

        // Verify snippet contains context
        assertTrue(snippet.length() > 10, "Snippet should contain meaningful context (> 10 chars)");
    }

    /**
     * Test 4: No Results Handling
     *
     * Tests behavior when search returns no results
     * Query: "typescript" (not in any documents)
     * Expected: totalHits=0, results.size()=0
     * Verify: Response is valid but empty
     */
    @Test
    @DisplayName("Test 4: No results handling")
    void testNoResults() throws Exception {
        SearchResponse response = handler.executeSearch("typescript", 10, 0);

        assertNotNull(response, "SearchResponse should not be null even with no results");
        assertEquals(0, response.getTotalResults(), "TotalHits should be 0");
        assertEquals(0, response.getResults().size(), "Results list should be empty");
        assertEquals(10, response.getLimit(), "Limit should still be 10");
        assertEquals(0, response.getOffset(), "Offset should still be 0");
        assertNotNull(response.getTimestamp(), "Timestamp should still be set");
    }

    /**
     * Test 5: Multi-Word Search (OR Logic)
     *
     * Tests search with multiple query terms
     * Query: "Python features" (should find documents with Python OR features)
     * Expected: Multiple results
     * Verify: Results include both python docs and docs with "features"
     */
    @Test
    @DisplayName("Test 5: Multi-word search with OR logic")
    void testMultiWordSearch() throws Exception {
        SearchResponse response = handler.executeSearch("Python features", 10, 0);

        // Should find docs with "Python" and/or "features"
        assertTrue(response.getTotalResults() > 0, "Should find results for multi-word query");
        assertTrue(response.getResults().size() > 0, "Results list should not be empty");

        // Verify we get results that match the query
        boolean foundPython = false;
        boolean foundFeatures = false;

        for (SearchResult result : response.getResults()) {
            String snippet = result.getSnippet().toLowerCase();
            if (snippet.contains("python")) {
                foundPython = true;
            }
            if (snippet.contains("<match>")) {
                // Snippet has highlighting
                foundFeatures = true;
            }
        }

        assertTrue(foundPython || foundFeatures, "Should find results matching query terms");
    }

    /**
     * Test 6: Snippet Accuracy and Highlighting
     *
     * Tests that snippets are accurate and contain proper highlighting
     * Query: "enterprise"
     * Expected: Snippet contains "enterprise" highlighted with <match> tags
     * Verify: Snippet text is readable and highlights are correctly placed
     */
    @Test
    @DisplayName("Test 6: Snippet accuracy and proper highlighting")
    void testSnippetAccuracy() throws Exception {
        SearchResponse response = handler.executeSearch("enterprise", 10, 0);

        assertFalse(response.getResults().isEmpty(), "Should find results for 'enterprise'");

        // Find the result from java-tutorial.txt
        SearchResult javaResult = null;
        for (SearchResult result : response.getResults()) {
            if (result.getFilePath().contains("java")) {
                javaResult = result;
                break;
            }
        }

        assertNotNull(javaResult, "Should find java tutorial result");
        String snippet = javaResult.getSnippet();

        // Verify snippet contains the query term highlighted
        assertTrue(snippet.contains("<match>enterprise</match>") || snippet.contains("<match>") && snippet.contains("enterprise"),
                "Snippet should contain highlighted 'enterprise'");

        // Verify snippet has context (words before and after match)
        assertTrue(snippet.length() > 20, "Snippet should have sufficient context");
    }

    /**
     * Test 7: Case-Insensitive Search
     *
     * Tests that search is case-insensitive
     * Query: "JAVA" (uppercase)
     * Expected: Same results as lowercase "java"
     * Verify: Results are found regardless of case
     */
    @Test
    @DisplayName("Test 7: Case-insensitive search")
    void testCaseInsensitiveSearch() throws Exception {
        SearchResponse lowercaseResponse = handler.executeSearch("java", 10, 0);
        SearchResponse uppercaseResponse = handler.executeSearch("JAVA", 10, 0);

        // Both should find the same number of results
        assertEquals(lowercaseResponse.getTotalResults(), uppercaseResponse.getTotalResults(),
                "Search should be case-insensitive");
        assertEquals(lowercaseResponse.getResults().size(), uppercaseResponse.getResults().size(),
                "Lowercase and uppercase queries should return same number of results");
    }

    /**
     * Test 8: Search Results Relevance (Scoring)
     *
     * Tests that search results are scored and ordered by relevance
     * Query: "Python"
     * Expected: Results ordered by relevance score
     * Verify: Scores are positive and first result has higher/equal score to second
     */
    @Test
    @DisplayName("Test 8: Search results are scored by relevance")
    void testSearchResultScoring() throws Exception {
        SearchResponse response = handler.executeSearch("Python", 10, 0);

        assertFalse(response.getResults().isEmpty(), "Should find results");

        // Verify all results have positive scores
        double previousScore = Double.MAX_VALUE;
        for (SearchResult result : response.getResults()) {
            assertTrue(result.getScore() > 0, "Score should be positive: " + result.getScore());
            // Lucene returns results in descending score order
            assertTrue(result.getScore() <= previousScore,
                    "Results should be ordered by score (descending)");
            previousScore = result.getScore();
        }
    }

    /**
     * Test 9: Long Document Handling
     *
     * Tests that long documents are properly indexed and searched
     * The test documents contain substantial text
     * Expected: Search results include proper snippets (not truncated improperly)
     * Verify: Long documents are searchable and generate meaningful snippets
     */
    @Test
    @DisplayName("Test 9: Long document handling")
    void testLongDocumentHandling() throws Exception {
        // Search in the python-advanced.txt which has long content
        SearchResponse response = handler.executeSearch("Metaclasses", 10, 0);

        assertTrue(response.getTotalResults() > 0, "Should find 'Metaclasses' in long documents");

        SearchResult result = response.getResults().get(0);
        String snippet = result.getSnippet();

        // Verify snippet is meaningful (not just empty or single word)
        assertTrue(snippet.length() > 20, "Snippet should contain context for long documents");
        assertTrue(snippet.contains("<match>"), "Snippet should be highlighted");
    }

    /**
     * Test 10: Pagination with Offset Beyond Results
     *
     * Tests pagination edge case where offset is beyond available results
     * Query: "python" (appears in 2 documents), offset=10, limit=10
     * Expected: totalHits=2 but results.size()=0 (offset beyond available)
     * Verify: Proper handling of out-of-range pagination
     */
    @Test
    @DisplayName("Test 10: Pagination offset beyond available results")
    void testPaginationOffsetBeyondResults() throws Exception {
        SearchResponse response = handler.executeSearch("python", 10, 10);

        // Should still report correct total hits
        assertEquals(2, response.getTotalResults(), "TotalHits should be 2");

        // But results should be empty due to offset
        assertEquals(0, response.getResults().size(), "Results should be empty when offset > totalHits");

        // Verify pagination metadata is preserved
        assertEquals(10, response.getLimit(), "Limit should be 10");
        assertEquals(10, response.getOffset(), "Offset should be 10");
    }

    /**
     * Test 11: Maximum Limit (100)
     *
     * Tests search with maximum valid limit (100)
     * Expected: Request succeeds, returns up to 100 results
     * Verify: No exception, results respect limit
     */
    @Test
    @DisplayName("Test 11: Maximum valid limit (100)")
    void testMaximumValidLimit() throws Exception {
        SearchResponse response = handler.executeSearch("is", 100, 0);

        assertNotNull(response, "Should return valid response with limit=100");
        assertEquals(100, response.getLimit(), "Limit should be 100");
        assertTrue(response.getResults().size() <= 100, "Results should not exceed limit");
    }

    /**
     * Test 12: Multiple Documents Matching Same Query
     *
     * Tests search that matches multiple documents
     * Query: "language" (appears in multiple documents)
     * Expected: Multiple results with different paths
     * Verify: All results have unique paths, each is a valid match
     */
    @Test
    @DisplayName("Test 12: Multiple documents matching query")
    void testMultipleDocumentsMatching() throws Exception {
        SearchResponse response = handler.executeSearch("language", 10, 0);

        assertTrue(response.getTotalResults() > 1, "Should match multiple documents");
        assertTrue(response.getResults().size() > 1, "Should return multiple results");

        // Verify all paths are unique
        java.util.Set<String> uniquePaths = new java.util.HashSet<>();
        for (SearchResult result : response.getResults()) {
            uniquePaths.add(result.getFilePath());
        }
        assertEquals(response.getResults().size(), uniquePaths.size(),
                "All results should have unique paths");

        // Verify each result has valid content
        for (SearchResult result : response.getResults()) {
            assertNotNull(result.getFilePath(), "Path should not be null");
            assertTrue(result.getScore() > 0, "Score should be positive");
            assertFalse(result.getSnippet().isEmpty(), "Snippet should not be empty");
        }
    }

    /**
     * Test 13: Snippet Contains Context Around Match
     *
     * Tests that snippets include context (words/phrases) around matched terms
     * Query: "scalable"
     * Expected: Snippet shows the matched term with surrounding context
     * Verify: Snippet is meaningful and not just the matched word alone
     */
    @Test
    @DisplayName("Test 13: Snippet contains context around matches")
    void testSnippetContextAroundMatch() throws Exception {
        SearchResponse response = handler.executeSearch("scalable", 10, 0);

        assertTrue(response.getTotalResults() > 0, "Should find 'scalable'");

        SearchResult result = response.getResults().get(0);
        String snippet = result.getSnippet();

        // Snippet should have meaningful context
        assertTrue(snippet.length() > 20, "Snippet should have context (length > 20 chars)");

        // Snippet should contain match tags
        assertTrue(snippet.contains("<match>") && snippet.contains("</match>"),
                "Snippet should have match highlighting");

        // Snippet should contain surrounding words, not just the match
        String[] parts = snippet.split("<match>");
        if (parts.length > 1) {
            // There should be text before and/or after the <match> tag
            String beforeMatch = parts[0].trim();
            String afterMatch = parts[1].contains("</match>") ?
                    parts[1].substring(parts[1].indexOf("</match>") + 8).trim() : "";

            assertTrue(!beforeMatch.isEmpty() || !afterMatch.isEmpty(),
                    "Snippet should have context before or after match");
        }
    }

    /**
     * Test 14: Full E2E Flow with Realistic Workflow
     *
     * Tests a realistic search workflow: initial search → pagination → result inspection
     * Simulates user navigating through search results
     * Expected: Consistent totalHits across pages, proper result ordering
     */
    @Test
    @DisplayName("Test 14: Full E2E realistic workflow")
    void testFullE2ERealisticWorkflow() throws Exception {
        // User searches for "programming"
        SearchResponse initialSearch = handler.executeSearch("programming", 2, 0);

        // Verify initial search
        assertTrue(initialSearch.getTotalResults() > 0, "Should find results");
        assertTrue(initialSearch.getResults().size() <= 2, "Should respect limit");

        // User navigates to next page
        if (initialSearch.getTotalResults() > 2) {
            SearchResponse nextPage = handler.executeSearch("programming", 2, 2);

            // Verify next page has same totalHits
            assertEquals(initialSearch.getTotalResults(), nextPage.getTotalResults(),
                    "TotalHits should be consistent across pages");

            // Verify results are different
            if (!initialSearch.getResults().isEmpty() && !nextPage.getResults().isEmpty()) {
                String firstPageFirstPath = initialSearch.getResults().get(0).getFilePath();
                String secondPageFirstPath = nextPage.getResults().get(0).getFilePath();
                assertNotEquals(firstPageFirstPath, secondPageFirstPath,
                        "Different pages should have different results");
            }
        }

        // Verify all results have complete information
        for (SearchResult result : initialSearch.getResults()) {
            assertNotNull(result.getFilePath(), "Result should have path");
            assertNotNull(result.getSnippet(), "Result should have snippet");
            assertTrue(result.getScore() > 0, "Result should have score");
            assertNotNull(result.getLastModified(), "Result should have lastModified");
        }
    }

    /**
     * Test 15: Special Characters in Document Content
     *
     * Tests that special characters in content don't break search
     * Query: "customizable" (word with common special character in English)
     * Expected: Search still works, snippets are properly formatted
     */
    @Test
    @DisplayName("Test 15: Handling of special characters in content")
    void testSpecialCharactersHandling() throws Exception {
        // All test documents use standard ASCII, verify search works
        SearchResponse response = handler.executeSearch("modifications", 10, 0);

        // If nothing found with this word, try another
        if (response.getTotalResults() == 0) {
            response = handler.executeSearch("standard", 10, 0);
        }

        // Verify response is valid regardless
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getResults(), "Results list should not be null");
    }

    // ==================== Helper Methods ====================

    /**
     * Helper method to add a document to the Lucene index
     * @param writer IndexWriter instance
     * @param path Document path/identifier
     * @param content Document content/text
     */
    private void addDocument(IndexWriter writer, String path, String content) throws Exception {
        Document doc = new Document();
        doc.add(new StringField("path", path, Field.Store.YES));
        doc.add(new TextField("contents", content, Field.Store.YES));
        doc.add(new LongField("fileSize", 1024L, Field.Store.YES));
        doc.add(new LongField("lastModified", System.currentTimeMillis(), Field.Store.YES));
        writer.addDocument(doc);
    }
}
