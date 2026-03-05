package dev.notequest.search;

import dev.notequest.api.SearchResponse;
import dev.notequest.api.SearchResult;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class SearchResultHandler {

    private final IndexSearcher searcher;
    private final SnippetExtractor snippetExtractor;
    private final QueryParser queryParser;

    public SearchResultHandler(IndexSearcher searcher, SnippetExtractor snippetExtractor) {
        this.searcher = searcher;
        this.snippetExtractor = snippetExtractor;
        this.queryParser = new QueryParser();
    }

    /**
     * Execute search query and return paginated results with snippets.
     * @param queryString User query string
     * @param limit Results per page (1-100)
     * @param offset Pagination offset
     * @return SearchResponse with results + metadata
     */
    public SearchResponse executeSearch(String queryString, int limit, int offset) throws Exception {
        // Validate parameters
        if (limit <= 0 || limit > 100) {
            throw new IllegalArgumentException("Limit must be between 1 and 100");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("Offset cannot be negative");
        }

        // Parse query string into Lucene Query
        Query query = queryParser.parse(queryString);

        // Execute query - fetch enough results to cover pagination
        int totalResultsNeeded = limit + offset;
        TopDocs topDocs = searcher.search(query, Math.max(totalResultsNeeded, 1000));

        // Extract total hits count from TotalHits object using reflection
        long totalHitsCount;
        try {
            java.lang.reflect.Field valueField = org.apache.lucene.search.TotalHits.class.getDeclaredField("value");
            valueField.setAccessible(true);
            totalHitsCount = (long) valueField.get(topDocs.totalHits);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Fallback: use scoreDocs length as approximation
            totalHitsCount = topDocs.scoreDocs.length;
        }

        // Build results list
        List<SearchResult> results = new ArrayList<>();
        for (int i = offset; i < Math.min(offset + limit, topDocs.scoreDocs.length); i++) {
            ScoreDoc scoreDoc = topDocs.scoreDocs[i];
            Document doc = searcher.storedFields().document(scoreDoc.doc);
            SearchResult result = buildSearchResult(doc, scoreDoc.score, queryString);
            results.add(result);
        }

        // Build response
        return SearchResponse.builder()
                .results(results)
                .totalHits(totalHitsCount)
                .limit(limit)
                .offset(offset)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Build a SearchResult from Lucene Document and score.
     */
    private SearchResult buildSearchResult(Document doc, float score, String queryString) throws Exception {
        String path = doc.get("path");
        String content = doc.get("contents");

        // Extract snippet with highlighting
        String snippet = snippetExtractor.extractSnippet(content != null ? content : "", queryString);

        return SearchResult.builder()
                .path(path)
                .score((double) score)
                .lastModified(getLastModified(doc))
                .snippet(snippet)
                .build();
    }

    /**
     * Get last modified timestamp from document (if available).
     */
    private LocalDateTime getLastModified(Document doc) {
        String lastModStr = doc.get("lastModified");
        if (lastModStr == null || lastModStr.isEmpty()) {
            return LocalDateTime.now();
        }
        try {
            long millis = Long.parseLong(lastModStr);
            return LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(millis),
                    ZoneId.systemDefault()
            );
        } catch (NumberFormatException e) {
            return LocalDateTime.now();
        }
    }
}
