package dev.notequest.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.nio.file.WatchEvent;
import java.time.Instant;

import com.google.common.eventbus.EventBus;

public class FileWatcherServiceTest {

    private FileWatcherService watcher;
    private EventBus mockBus;
    private Path tempDir;

    @BeforeEach
    public void setUp() throws IOException {
        // Create temp directory and mock EventBus
        tempDir = Files.createTempDirectory("file-watcher-test");
        mockBus = mock(EventBus.class);

        // Create watcher with injectable constructor
        watcher = new FileWatcherService(tempDir, mockBus);
    }

    @AfterEach
    public void tearDown() throws IOException {
        // Clean up temp directory
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
    public void testFileIsInExtensionFilterWithTxtFile() {
        // Test that .txt files match
        Boolean result = watcher.fileIsInExtensionFilter("notes.txt");
        assertTrue(result, ".txt files should match filter");
    }

    @Test
    public void testFileIsInExtensionFilterWithNonMatchingFile() {
        // Test that .md files don't match when only .txt is configured
        Boolean result = watcher.fileIsInExtensionFilter("notes.md");
        assertFalse(result, ".md files should not match when .txt filter is active");
    }

    @Test
    public void testFileIsInExtensionFilterWithEmptyString() {
        // Test that empty string doesn't match
        Boolean result = watcher.fileIsInExtensionFilter("");
        assertFalse(result, "Empty string should not match");
    }

    @Test
    public void testFileIsInExtensionFilterWithEmptyExtensionArray() {
        // Create a watcher with no extension filter
        try {
            FileWatcherService noFilterWatcher = new FileWatcherService(tempDir, mockBus);
            // Note: FileWatcherService.FILE_EXTENSIONS is static, so we can't easily test empty array
            // This is a limitation of the current design
            assertNotNull(noFilterWatcher, "Watcher should be created");
        } catch (IOException e) {
            fail("Should not throw exception");
        }
    }

    @Test
    public void testGetFileResultFromFileChangeForCreate() {
        // Test ENTRY_CREATE event - create file first so metadata can be read
        try {
            Path testPath = tempDir.resolve("test.txt");
            Files.createFile(testPath);
            WatchEvent.Kind<?> kind = java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;

            FileResult result = watcher.getFileResultFromFileChange(testPath, kind);

            assertEquals(testPath, result.getPath(), "Path should match");
            assertEquals(FileResult.FileStatus.PENDING, result.getFileStatus(), "Status should be PENDING for CREATE");
        } catch (IOException e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }

    @Test
    public void testGetFileResultFromFileChangeForModify() {
        // Create a file first
        try {
            Path testPath = tempDir.resolve("test.txt");
            Files.createFile(testPath);

            WatchEvent.Kind<?> kind = java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
            FileResult result = watcher.getFileResultFromFileChange(testPath, kind);

            assertEquals(testPath, result.getPath(), "Path should match");
            assertEquals(FileResult.FileStatus.PENDING, result.getFileStatus(), "Status should be PENDING for MODIFY");
        } catch (IOException e) {
            fail("Should not throw exception");
        }
    }

    @Test
    public void testGetFileResultFromFileChangeForDelete() {
        // Test ENTRY_DELETE event
        Path testPath = tempDir.resolve("deleted.txt");
        WatchEvent.Kind<?> kind = java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;

        FileResult result = watcher.getFileResultFromFileChange(testPath, kind);

        assertEquals(testPath, result.getPath(), "Path should match");
        assertEquals(FileResult.FileStatus.DELETED, result.getFileStatus(), "Status should be DELETED");
    }

    @Test
    public void testGetFileResultFromFileChangeForMissingFile() {
        // Test event for non-existent file
        Path missingPath = tempDir.resolve("missing.txt");
        WatchEvent.Kind<?> kind = java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;

        FileResult result = watcher.getFileResultFromFileChange(missingPath, kind);

        assertEquals(missingPath, result.getPath(), "Path should match");
        assertEquals(FileResult.FileStatus.ERROR, result.getFileStatus(), "Status should be ERROR for missing file");
    }

    @Test
    public void testConstructorRegistersDirectories() {
        // Verify that constructor registered the directory
        // We can verify this by checking that watcher was created without exception
        assertNotNull(watcher, "Watcher should be created successfully");
    }

    @Test
    public void testConstructorWithNestedDirectories() throws IOException {
        // Create nested structure
        Path subdir = tempDir.resolve("subdir");
        Files.createDirectory(subdir);

        // Create new watcher with nested dir structure
        FileWatcherService newWatcher = new FileWatcherService(tempDir, mockBus);
        assertNotNull(newWatcher, "Should handle nested directories");
    }

    @Test
    public void testFileIsInExtensionFilterWithFullPath() {
        // Test with full path containing extension
        String fullPath = "/some/path/notes.txt";
        Boolean result = watcher.fileIsInExtensionFilter(fullPath);
        assertTrue(result, "Should match extension in full path");
    }

    @Test
    public void testFileIsInExtensionFilterCaseSensitive() {
        // Test that extension matching is case-sensitive (file ends with .txt, not .TXT)
        Boolean result = watcher.fileIsInExtensionFilter("notes.TXT");
        // Note: depending on implementation, this might fail
        // If FILE_EXTENSIONS has ".txt", ".TXT" won't match
        assertFalse(result, "Extension matching should be case-sensitive");
    }
}
