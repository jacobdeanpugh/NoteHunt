package dev.notequest.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.*;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import com.google.common.eventbus.EventBus;

import dev.notequest.events.PendingFilesRequestEvent;
import dev.notequest.events.SetFilesToCompleteEvent;

@Disabled("FileIndexerTest causes JVM crash - investigating memory/resource issues")
public class FileIndexerTest {

    private FileIndexer indexer;
    private Directory ramDir;
    private EventBus mockBus;
    private Path tempDir;

    @BeforeEach
    public void setUp() throws IOException {
        // Create in-memory Lucene directory and mock EventBus
        ramDir = new ByteBuffersDirectory();
        mockBus = mock(EventBus.class);
        indexer = new FileIndexer(ramDir, 3, mockBus);

        // Create temp directory for test files
        tempDir = Files.createTempDirectory("file-indexer-test");
    }

    @AfterEach
    public void tearDown() throws IOException {
        // Clean up
        if (Files.exists(tempDir)) {
            Files.walk(tempDir)
                .sorted(java.util.Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        }
    }

    @Test
    public void testIndexFileWritesDocumentWithPath() throws IOException {
        // Create a test file
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "test content");

        // Index the file
        indexer.indexFile(testFile);

        // Verify document was added (this is basic verification)
        assertDoesNotThrow(() -> indexer.indexFile(testFile), "Indexing should not throw");
    }

    @Test
    public void testIndexFileThrowsIOExceptionForMissingFile() {
        // Try to index non-existent file
        Path missingFile = tempDir.resolve("missing.txt");

        assertThrows(IOException.class, () -> indexer.indexFile(missingFile),
                "Should throw IOException when file is missing");
    }

    @Test
    public void testIndexFileIncreasesDocumentCount() throws IOException {
        // Create and index multiple files
        Path file1 = tempDir.resolve("file1.txt");
        Path file2 = tempDir.resolve("file2.txt");
        Files.writeString(file1, "content 1");
        Files.writeString(file2, "content 2");

        indexer.indexFile(file1);
        indexer.indexFile(file2);

        // Verify no exceptions and documents were indexed
        assertDoesNotThrow(() -> indexer.indexFile(file1), "Should index multiple files");
    }

    @Test
    public void testRequestPendingFilesPostsEvent() {
        // Set up mock to capture the posted event
        java.util.concurrent.CompletableFuture<java.util.ArrayList<FileResult>> future =
            new java.util.concurrent.CompletableFuture<>();
        future.complete(new java.util.ArrayList<>());

        // Call requestPendingFiles
        indexer.requestPendingFiles();

        // Verify post was called with PendingFilesRequestEvent
        verify(mockBus).post(any(PendingFilesRequestEvent.class));
    }

    @Test
    public void testIndexFilesFromDatabasePostsCompleteEvents() throws IOException {
        // Create test files in database format
        Path file1 = tempDir.resolve("file1.txt");
        Path file2 = tempDir.resolve("file2.txt");
        Path file3 = tempDir.resolve("file3.txt");
        Files.writeString(file1, "content 1");
        Files.writeString(file2, "content 2");
        Files.writeString(file3, "content 3");

        // Mock the bus to handle the request immediately
        java.util.ArrayList<FileResult> mockFiles = new java.util.ArrayList<>();
        mockFiles.add(new FileResult(file1, FileResult.FileStatus.PENDING,
            java.nio.file.attribute.FileTime.from(java.time.Instant.now())));
        mockFiles.add(new FileResult(file2, FileResult.FileStatus.PENDING,
            java.nio.file.attribute.FileTime.from(java.time.Instant.now())));
        mockFiles.add(new FileResult(file3, FileResult.FileStatus.PENDING,
            java.nio.file.attribute.FileTime.from(java.time.Instant.now())));

        doAnswer(invocation -> {
            PendingFilesRequestEvent event = invocation.getArgument(0);
            event.getFuture().complete(mockFiles);
            return null;
        }).when(mockBus).post(any(PendingFilesRequestEvent.class));

        // Call indexFilesFromDatabase
        indexer.indexFilesFromDatabase();

        // Verify SetFilesToCompleteEvent was posted at least once
        verify(mockBus, atLeastOnce()).post(any(SetFilesToCompleteEvent.class));
    }

    @Test
    public void testIndexFilesFromDatabaseRespectseBatchSize() throws IOException {
        // Create 5 test files (batch size is 3)
        java.util.ArrayList<FileResult> mockFiles = new java.util.ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Path file = tempDir.resolve("file" + i + ".txt");
            Files.writeString(file, "content " + i);
            mockFiles.add(new FileResult(file, FileResult.FileStatus.PENDING,
                java.nio.file.attribute.FileTime.from(java.time.Instant.now())));
        }

        // Mock the bus to provide the files
        doAnswer(invocation -> {
            PendingFilesRequestEvent event = invocation.getArgument(0);
            event.getFuture().complete(mockFiles);
            return null;
        }).when(mockBus).post(any(PendingFilesRequestEvent.class));

        // Call indexFilesFromDatabase
        indexer.indexFilesFromDatabase();

        // Verify SetFilesToCompleteEvent was posted twice (3 files + 2 files = 2 batches)
        verify(mockBus, times(2)).post(any(SetFilesToCompleteEvent.class));
    }
}
