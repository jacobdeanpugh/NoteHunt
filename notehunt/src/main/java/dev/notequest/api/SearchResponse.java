package dev.notequest.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResponse {
    private List<SearchResult> results;
    private long totalResults;
    private int limit;
    private int offset;
    private LocalDateTime timestamp;
}
