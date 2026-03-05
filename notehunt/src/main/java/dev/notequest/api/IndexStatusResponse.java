package dev.notequest.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndexStatusResponse {
    private long filesIndexed;
    private long pendingFiles;
    private String indexSize;
    private LocalDateTime lastUpdated;
    private LocalDateTime timestamp;
}
