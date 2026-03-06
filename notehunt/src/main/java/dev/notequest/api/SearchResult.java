package dev.notequest.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResult {
    private String filePath;
    private double score;
    private LocalDateTime lastModified;
    private String snippet;
    private long fileSize;
    @Builder.Default
    private List<String> tags = Collections.unmodifiableList(new ArrayList<>());
}
