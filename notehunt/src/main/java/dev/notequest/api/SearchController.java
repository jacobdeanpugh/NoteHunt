package dev.notequest.api;

import dev.notequest.search.SearchResultHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SearchController {

    private final SearchResultHandler searchResultHandler;

    /**
     * Search endpoint: GET /search?q=<query>&limit=10&offset=0
     */
    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam(value = "q", required = true) String query,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @RequestParam(value = "offset", defaultValue = "0") int offset) {

        try {
            // Validate parameters
            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Query parameter 'q' is required", 400, LocalDateTime.now()));
            }

            if (limit < 1 || limit > 100) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Limit must be between 1 and 100", 400, LocalDateTime.now()));
            }

            if (offset < 0) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Offset cannot be negative", 400, LocalDateTime.now()));
            }

            // Execute search
            SearchResponse response = searchResultHandler.executeSearch(query.trim(), limit, offset);

            log.info("Search executed: query='{}', results={}, totalResults={}", query, response.getResults().size(), response.getTotalResults());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid query: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Invalid query: " + e.getMessage(), 400, LocalDateTime.now()));
        } catch (ParseException e) {
            log.warn("Query parse error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Query syntax error: " + e.getMessage(), 400, LocalDateTime.now()));
        } catch (Exception e) {
            log.error("Search error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Search failed: " + e.getMessage(), 500, LocalDateTime.now()));
        }
    }

    /**
     * Health check: GET /health
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(HealthResponse.builder()
                .status("UP")
                .indexing(false)  // TODO: Get from FileIndexer
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * Index status: GET /index/status
     */
    @GetMapping("/index/status")
    public ResponseEntity<IndexStatusResponse> indexStatus() {
        return ResponseEntity.ok(IndexStatusResponse.builder()
                .filesIndexed(0)  // TODO: Get from DatabaseHandler
                .pendingFiles(0)  // TODO: Get from DatabaseHandler
                .indexSize("0 MB")  // TODO: Calculate from index
                .lastUpdated(LocalDateTime.now())
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * Handle missing required request parameters.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException e) {
        log.warn("Missing required parameter: {}", e.getParameterName());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("Query parameter 'q' is required", 400, LocalDateTime.now()));
    }

    /**
     * Global exception handler for uncaught exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Internal server error", 500, LocalDateTime.now()));
    }
}
