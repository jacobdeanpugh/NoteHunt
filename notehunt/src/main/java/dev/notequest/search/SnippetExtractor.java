package dev.notequest.search;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;

public class SnippetExtractor {

    private static final int MAX_SNIPPET_LENGTH = 150;
    private static final int CONTEXT_CHARS = 50;
    private final QueryParser queryParser;

    public SnippetExtractor() {
        this.queryParser = new QueryParser();
    }

    public SnippetExtractor(QueryParser queryParser) {
        this.queryParser = queryParser;
    }

    /**
     * Extract snippet from text with context around matches.
     * @param text Full document text
     * @param queryString User query string (e.g., "python tutorial")
     * @return Snippet with <match> tags around matching terms
     */
    public String extractSnippet(String text, String queryString) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        try {
            Query query = queryParser.parse(queryString);
            String highlighted = highlightMatches(text, query);
            return truncateSnippet(highlighted, text.length());
        } catch (Exception e) {
            // Fallback: return first N chars if highlighting fails
            return text.substring(0, Math.min(MAX_SNIPPET_LENGTH, text.length()));
        }
    }

    /**
     * Highlight matched query terms in text with <match> tags.
     * @param text Full text to search
     * @param query Lucene query
     * @return Text with <match>term</match> around matches
     */
    public String highlightMatches(String text, Query query) throws Exception {
        SimpleHTMLFormatter formatter = new SimpleHTMLFormatter("<match>", "</match>");
        QueryScorer scorer = new QueryScorer(query);
        Highlighter highlighter = new Highlighter(formatter, scorer);

        StandardAnalyzer analyzer = new StandardAnalyzer();
        String highlighted = highlighter.getBestFragment(analyzer, "contents", text);

        // If no fragments found (no matches), return original text
        return highlighted != null ? highlighted : text;
    }

    /**
     * Truncate long snippets to max length, trying to end at word boundary.
     * @param snippet Text to truncate
     * @param originalTextLength Length of the original document text
     * @return Truncated snippet with ellipsis if needed
     */
    private String truncateSnippet(String snippet, int originalTextLength) {
        // If snippet is much shorter than original, add ellipsis to indicate more content exists
        if (snippet.length() < originalTextLength && !snippet.endsWith("...")) {
            // Check if we should add ellipsis
            if (snippet.length() <= MAX_SNIPPET_LENGTH) {
                return snippet + "...";
            }
        }

        if (snippet.length() <= MAX_SNIPPET_LENGTH) {
            return snippet;
        }

        String truncated = snippet.substring(0, MAX_SNIPPET_LENGTH);

        // Try to find last space to avoid cutting words
        int lastSpace = truncated.lastIndexOf(' ');
        if (lastSpace > MAX_SNIPPET_LENGTH / 2) {
            truncated = truncated.substring(0, lastSpace);
        }

        return truncated + "...";
    }
}
