package dev.notequest.config;

import dev.notequest.service.FileIndexer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Initializes file indexing when Spring Boot application starts.
 * Automatically scans the configured directory and indexes pending files.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IndexingStartupComponent {

    private final FileIndexer fileIndexer;

    /**
     * Triggered when Spring Boot application has fully started.
     * Initiates the file indexing process.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Application started. Beginning file indexing...");
        try {
            fileIndexer.indexFilesFromDatabase();
            log.info("File indexing completed successfully.");
        } catch (Exception e) {
            log.error("Error during file indexing", e);
        }
    }
}
