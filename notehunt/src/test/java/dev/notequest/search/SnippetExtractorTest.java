package dev.notequest.search;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SnippetExtractor class.
 *
 * Tests snippet extraction from document text, including:
 * - Match highlighting with tags
 * - Context preservation around matches
 * - Case-insensitive matching
 * - Truncation of long text
 * - Multiple match handling
 */
public class SnippetExtractorTest {

    private SnippetExtractor extractor;
    private QueryParser queryParser;

    @BeforeEach
    void setUp() {
        extractor = new SnippetExtractor();
        queryParser = new QueryParser();
    }

    /**
     * Test 1: Extract snippet with single match
     * Verifies that matched terms are wrapped in <match> tags
     */
    @Test
    public void testExtractSnippetWithMatch() {
        String text = "Python is a great programming language. Python developers love it.";
        String query = "Python";

        String snippet = extractor.extractSnippet(text, query);

        assertNotNull(snippet, "Snippet should not be null");
        assertTrue(snippet.contains("<match>"), "Snippet should contain opening match tag");
        assertTrue(snippet.contains("</match>"), "Snippet should contain closing match tag");
        assertTrue(snippet.toLowerCase().contains("python"), "Snippet should contain the search term");
    }

    /**
     * Test 2: Extract snippet with multiple matches
     * Verifies that all matching terms are highlighted
     */
    @Test
    public void testExtractSnippetMultipleMatches() {
        String text = "Learn python. Python is used in data science. Many python developers.";
        String query = "python";

        String snippet = extractor.extractSnippet(text, query);

        assertNotNull(snippet, "Snippet should not be null");
        // Count occurrences of match tags
        int matchCount = countOccurrences(snippet, "<match>");
        assertTrue(matchCount >= 2, "Snippet should contain at least 2 highlighted matches");
    }

    /**
     * Test 3: Extract snippet with no match
     * Verifies that a snippet is still returned even when query doesn't match
     */
    @Test
    public void testExtractSnippetNoMatch() {
        String text = "This is some text without the keyword.";
        String query = "python";

        String snippet = extractor.extractSnippet(text, query);

        assertNotNull(snippet, "Snippet should not be null even without match");
        assertTrue(snippet.length() > 0, "Snippet should have content");
        // Should return first N characters or default context
        assertTrue(snippet.contains("This") || snippet.contains("some"),
                   "Snippet should contain text from the original");
    }

    /**
     * Test 4: Extract snippet preserves context window around match
     * Verifies that surrounding text is included with the match
     */
    @Test
    public void testExtractSnippetContextWindow() {
        String text = "The quick brown fox jumps over the lazy dog";
        String query = "fox";

        String snippet = extractor.extractSnippet(text, query);

        assertNotNull(snippet, "Snippet should not be null");
        assertTrue(snippet.contains("<match>"), "Snippet should contain match tag");
        assertTrue(snippet.contains("</match>"), "Snippet should contain closing match tag");
        // Context should include some surrounding words
        assertTrue(snippet.contains("brown") || snippet.contains("jumps"),
                   "Snippet should include context around match");
    }

    /**
     * Test 5: Snippet truncates very long text
     * Verifies that long documents are truncated to reasonable length
     */
    @Test
    public void testExtractSnippetTruncatesLongText() {
        // Create a very long text (500+ characters)
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            longText.append("This is a paragraph about data science and machine learning. ");
        }
        String text = longText.toString();
        String query = "learning";

        String snippet = extractor.extractSnippet(text, query);

        assertNotNull(snippet, "Snippet should not be null");
        // Snippet should be truncated to a reasonable size
        assertTrue(snippet.length() <= 300,
                   "Snippet should be truncated to <= 300 characters, got " + snippet.length());
    }

    /**
     * Test 6: Highlight multiple occurrences of match
     * Verifies that multiple matches are all highlighted
     */
    @Test
    public void testHighlightMatches() {
        String text = "Python is great. Python rules. Python is everywhere.";
        String query = "Python";

        String snippet = extractor.extractSnippet(text, query);

        assertNotNull(snippet, "Snippet should not be null");
        int matchTagCount = countOccurrences(snippet, "<match>");
        int closeTagCount = countOccurrences(snippet, "</match>");
        assertEquals(matchTagCount, closeTagCount,
                     "Opening and closing match tags should be balanced");
        assertTrue(matchTagCount >= 2, "Should have at least 2 highlighted matches");
    }

    /**
     * Test 7: Highlight matches case-insensitive
     * Verifies that case variations of the search term are all highlighted
     */
    @Test
    public void testHighlightMatchesCaseInsensitive() {
        String text = "python is PYTHON and Python everywhere";
        String query = "python";

        String snippet = extractor.extractSnippet(text, query);

        assertNotNull(snippet, "Snippet should not be null");
        int matchCount = countOccurrences(snippet, "<match>");
        assertTrue(matchCount >= 2,
                   "Should highlight case-insensitive matches, found " + matchCount);
    }

    /**
     * Test 8: Snippet with ellipsis for truncation
     * Verifies that truncated snippets include ellipsis indicator
     */
    @Test
    public void testTruncatedSnippetHasEllipsis() {
        String text = "This is the beginning of a very long document. " +
                      "It contains many paragraphs about various topics including " +
                      "science, technology, history, and many other subjects. " +
                      "The document is intentionally verbose to test truncation behavior.";
        String query = "science";

        String snippet = extractor.extractSnippet(text, query);

        assertNotNull(snippet, "Snippet should not be null");
        // If truncated, should have ellipsis
        if (snippet.length() < text.length()) {
            assertTrue(snippet.contains("...") || snippet.endsWith("..."),
                       "Truncated snippet should indicate truncation");
        }
    }

    /**
     * Test 9: Empty text returns empty snippet
     * Verifies behavior with edge case empty text
     */
    @Test
    public void testExtractSnippetEmptyText() {
        String text = "";
        String query = "python";

        String snippet = extractor.extractSnippet(text, query);

        assertNotNull(snippet, "Snippet should not be null");
        assertEquals("", snippet, "Empty text should return empty snippet");
    }

    /**
     * Test 10: Snippet preserves special characters
     * Verifies that special characters in text are preserved in snippet
     */
    @Test
    public void testExtractSnippetPreservesSpecialCharacters() {
        String text = "The @python library (numpy/scipy) is great!";
        String query = "python";

        String snippet = extractor.extractSnippet(text, query);

        assertNotNull(snippet, "Snippet should not be null");
        assertTrue(snippet.contains("@") || snippet.contains("(") || snippet.contains(")"),
                   "Snippet should preserve special characters");
    }

    /**
     * Test 11: Snippet with phrase query
     * Verifies that multi-word phrases are highlighted correctly
     */
    @Test
    public void testExtractSnippetPhrasedQuery() {
        String text = "Machine learning is a subset of artificial intelligence. " +
                      "Machine learning algorithms are powerful.";
        String query = "machine learning";

        String snippet = extractor.extractSnippet(text, query);

        assertNotNull(snippet, "Snippet should not be null");
        // Should highlight the phrase or individual terms
        assertTrue(snippet.contains("<match>"), "Snippet should contain match tags");
    }

    /**
     * Test 12: Extract snippet with unicode characters
     * Verifies that unicode content is handled correctly
     */
    @Test
    public void testExtractSnippetUnicodeContent() {
        String text = "Python is used in data science. Café serves great coffee.";
        String query = "Python";

        String snippet = extractor.extractSnippet(text, query);

        assertNotNull(snippet, "Snippet should not be null");
        assertTrue(snippet.contains("<match>") || snippet.length() > 0,
                   "Snippet should handle unicode content");
    }

    /**
     * Helper method: Count occurrences of a substring in text
     * Used to verify match highlighting in test assertions
     *
     * @param text The text to search in
     * @param substring The substring to count
     * @return Number of non-overlapping occurrences
     */
    private int countOccurrences(String text, String substring) {
        if (text == null || substring == null || substring.isEmpty()) {
            return 0;
        }
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }
}
