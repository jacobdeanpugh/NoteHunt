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
    // Primary fields (used by IndexStatusScreen - left side panel)
    private long completedFiles;    // renamed from filesIndexed
    private long pendingFiles;      // detailed breakdown
    private long inProgressFiles;   // new
    private long errorFiles;        // new
    private LocalDateTime lastSyncTime; // renamed from lastUpdated

    // Alias fields (used by SummaryCards - top cards showing total, complete, pending, error)
    private long total;             // total = completed + pending + inProgress + error
    private long complete;          // alias for completedFiles
    private long pending;           // alias for pendingFiles
    private long error;             // alias for errorFiles

    // Metadata
    private String indexSize;
    private LocalDateTime timestamp;
}
