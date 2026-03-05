package dev.notequest.search;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;

public class QueryParser {
    private static final String[] FIELDS = {"path", "contents"};
    private final MultiFieldQueryParser multiFieldParser;

    public QueryParser() {
        this.multiFieldParser = new MultiFieldQueryParser(FIELDS, new StandardAnalyzer());
        // Default to OR between terms for broader results
        this.multiFieldParser.setDefaultOperator(MultiFieldQueryParser.OR_OPERATOR);
    }

    /**
     * Parse user query string into Lucene Query object.
     * @param queryString User input (e.g., "python tutorial")
     * @return Lucene Query
     * @throws IllegalArgumentException if query is empty/null
     * @throws ParseException if query syntax is invalid
     */
    public Query parse(String queryString) throws ParseException {
        if (!validateQuery(queryString)) {
            throw new IllegalArgumentException("Query cannot be null or empty");
        }

        String trimmed = queryString.trim();

        try {
            return multiFieldParser.parse(trimmed);
        } catch (ParseException e) {
            throw new ParseException("Invalid query syntax: " + e.getMessage());
        }
    }

    /**
     * Validate query string before parsing.
     * @param queryString Query to validate
     * @return true if valid, false otherwise
     */
    public boolean validateQuery(String queryString) {
        return queryString != null && !queryString.trim().isEmpty();
    }
}
