package dev.notequest.config;

import dev.notequest.search.SearchResultHandler;
import dev.notequest.service.FileIndexer;
import dev.notequest.service.FileTreeCrawler;
import dev.notequest.handler.DatabaseHandler;
import dev.notequest.handler.EventBusRegistry;
import dev.notequest.events.FileTreeCrawledEvent;
import dev.notequest.util.ConfigProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.lucene.search.IndexSearcher;

/**
 * Initializes file indexing when Spring Boot application starts.
 * Automatically scans the configured directory and indexes pending files.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IndexingStartupComponent {

    private final FileIndexer fileIndexer;
    private final DatabaseHandler databaseHandler;
    private final SearchResultHandler searchResultHandler;

    /**
     * Triggered when Spring Boot application has fully started.
     * Performs initial directory scan, populates database, then indexes files.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Application started. Registering event subscribers...");
        // Register DatabaseHandler with EventBus to handle file events
        EventBusRegistry.bus().register(databaseHandler);
        log.info("Event subscribers registered.");

        try {
            // Step 1: Scan directory and populate database
            log.info("Scanning directory for files...");
            String directoryPath = ConfigProvider.instance.getDirectoryPath();
            String[] extensions = {FileTreeCrawler.FileExtention.MD, FileTreeCrawler.FileExtention.TXT};

            FileTreeCrawler crawler = new FileTreeCrawler(extensions);
            Files.walkFileTree(Paths.get(directoryPath), crawler);

            // Post event to populate database with crawled files
            FileTreeCrawledEvent event = new FileTreeCrawledEvent(crawler.getResults());
            EventBusRegistry.bus().post(event);
            log.info("Directory scanned and database populated.");

            // Step 2: Index files from database
            log.info("Beginning file indexing...");
            fileIndexer.indexFilesFromDatabase();
            log.info("File indexing completed successfully.");

            // Step 3: Refresh the searcher in SearchResultHandler with the newly populated index
            try {
                IndexSearcher refreshedSearcher = fileIndexer.getSearcher();
                searchResultHandler.refreshSearcher(refreshedSearcher);
                log.info("Search index refreshed and ready for queries.");
            } catch (Exception e) {
                log.error("Error refreshing search index", e);
            }
        } catch (Exception e) {
            log.error("Error during startup indexing process", e);
        }
    }
}
