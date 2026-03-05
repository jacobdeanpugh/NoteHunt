package dev.notequest.search;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.apache.lucene.search.Query;
import org.apache.lucene.queryparser.classic.ParseException;

public class QueryParserTest {

    @Test
    public void testParseSimpleWord() throws ParseException {
        // Test parsing a single word query
        String input = "python";
        QueryParser parser = new QueryParser();

        Query query = parser.parse(input);

        assertNotNull(query, "Query should not be null");
        assertTrue(query.toString().contains("python"), "Query should contain 'python'");
    }

    @Test
    public void testParseMultipleWords() throws ParseException {
        // Test parsing multiple words - should create AND query
        String input = "machine learning";
        QueryParser parser = new QueryParser();

        Query query = parser.parse(input);

        assertNotNull(query, "Query should not be null");
        String queryStr = query.toString();
        assertTrue(queryStr.contains("machine"), "Query should contain 'machine'");
        assertTrue(queryStr.contains("learning"), "Query should contain 'learning'");
    }

    @Test
    public void testParseWithLeadingTrailingSpaces() throws ParseException {
        // Test parsing with leading and trailing whitespace
        String input = "  python  ";
        QueryParser parser = new QueryParser();

        Query query = parser.parse(input);

        assertNotNull(query, "Query should not be null");
        assertTrue(query.toString().contains("python"), "Query should contain 'python'");
    }

    @Test
    public void testParseEmptyString() {
        // Test parsing empty string throws exception
        String input = "";
        QueryParser parser = new QueryParser();

        assertThrows(IllegalArgumentException.class, () -> {
            parser.parse(input);
        }, "Parsing empty string should throw IllegalArgumentException");
    }

    @Test
    public void testParseNull() {
        // Test parsing null throws exception
        QueryParser parser = new QueryParser();

        assertThrows(IllegalArgumentException.class, () -> {
            parser.parse(null);
        }, "Parsing null should throw IllegalArgumentException");
    }

    @Test
    public void testParseSpecialCharacters() throws ParseException {
        // Test parsing query with special characters
        String input = "hello-world";
        QueryParser parser = new QueryParser();

        Query query = parser.parse(input);

        assertNotNull(query, "Query should not be null");
        assertTrue(query.toString().length() > 0, "Query should have content");
    }

    @Test
    public void testParseAccentedCharacters() throws ParseException {
        // Test parsing query with accented characters
        String input = "café";
        QueryParser parser = new QueryParser();

        Query query = parser.parse(input);

        assertNotNull(query, "Query should not be null");
        assertTrue(query.toString().length() > 0, "Query should have content");
    }

    @Test
    public void testValidateQueryValid() {
        // Test validation returns true for valid query
        String input = "hello world";
        QueryParser parser = new QueryParser();

        boolean isValid = parser.validateQuery(input);

        assertTrue(isValid, "Valid query should return true");
    }

    @Test
    public void testValidateQueryEmpty() {
        // Test validation returns false for empty query
        String input = "";
        QueryParser parser = new QueryParser();

        boolean isValid = parser.validateQuery(input);

        assertFalse(isValid, "Empty query should return false");
    }

    @Test
    public void testValidateQueryNull() {
        // Test validation returns false for null query
        QueryParser parser = new QueryParser();

        boolean isValid = parser.validateQuery(null);

        assertFalse(isValid, "Null query should return false");
    }

    @Test
    public void testParseVeryLongQuery() throws ParseException {
        // Test parsing very long query string
        String input = "the quick brown fox jumps over the lazy dog and many other words to make it a very long query string";
        QueryParser parser = new QueryParser();

        Query query = parser.parse(input);

        assertNotNull(query, "Query should not be null for long input");
        assertTrue(query.toString().length() > 0, "Query should have content");
    }
}
