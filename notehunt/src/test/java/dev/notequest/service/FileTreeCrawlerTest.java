package dev.notequest.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

import dev.notequest.service.FileResult.FileStatus;

public class FileTreeCrawlerTest {

    private Path tempDir;

    @BeforeEach
    public void setUp() throws IOException {
        // Create a temporary directory for testing
        tempDir = Files.createTempDirectory("file-tree-crawler-test");
    }

    @AfterEach
    public void tearDown() throws IOException {
        // Clean up temporary directory
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
    public void testFilteredModeIncludesTxtFiles() throws IOException {
        // Create .txt and .md files
        Files.createFile(tempDir.resolve("file1.txt"));
        Files.createFile(tempDir.resolve("file2.txt"));
        Files.createFile(tempDir.resolve("file3.md"));

        FileTreeCrawler crawler = new FileTreeCrawler(new String[]{".txt"});
        Files.walkFileTree(tempDir, crawler);

        ArrayList<FileResult> results = crawler.getResults();
        assertEquals(2, results.size(), "Should find only 2 .txt files");
    }

    @Test
    public void testFilteredModeExcludesMdFiles() throws IOException {
        // Create .txt and .md files
        Files.createFile(tempDir.resolve("file1.txt"));
        Files.createFile(tempDir.resolve("file2.md"));

        FileTreeCrawler crawler = new FileTreeCrawler(new String[]{".txt"});
        Files.walkFileTree(tempDir, crawler);

        ArrayList<FileResult> results = crawler.getResults();
        assertTrue(results.stream().noneMatch(r -> r.getPath().toString().endsWith(".md")),
                ".md files should not be included");
    }

    @Test
    public void testUnfilteredModeIncludesAllFiles() throws IOException {
        // Create files with different extensions
        Files.createFile(tempDir.resolve("file1.txt"));
        Files.createFile(tempDir.resolve("file2.md"));
        Files.createFile(tempDir.resolve("file3.java"));

        FileTreeCrawler crawler = new FileTreeCrawler(); // No-arg constructor, no filtering
        Files.walkFileTree(tempDir, crawler);

        ArrayList<FileResult> results = crawler.getResults();
        assertEquals(3, results.size(), "Should find all 3 files");
    }

    @Test
    public void testVisitFileFailedRecordsError() throws IOException {
        // Create a file
        Path file = tempDir.resolve("test.txt");
        Files.createFile(file);

        // Simulate a failed visit by calling visitFileFailed directly
        FileTreeCrawler crawler = new FileTreeCrawler(new String[]{".txt"});
        IOException testException = new IOException("Simulated read error");
        crawler.visitFileFailed(file, testException);

        ArrayList<FileResult> results = crawler.getResults();
        assertEquals(1, results.size(), "Should have one error record");
        assertEquals(FileStatus.ERROR, results.get(0).getFileStatus(), "Should be marked as ERROR");
    }

    @Test
    public void testEmptyDirectoryReturnsEmptyResults() throws IOException {
        // Don't create any files
        FileTreeCrawler crawler = new FileTreeCrawler(new String[]{".txt"});
        Files.walkFileTree(tempDir, crawler);

        ArrayList<FileResult> results = crawler.getResults();
        assertEquals(0, results.size(), "Empty directory should return no results");
    }

    @Test
    public void testNestedSubdirectoryFilesAreFound() throws IOException {
        // Create nested directory structure
        Path subdir = tempDir.resolve("subdir");
        Files.createDirectory(subdir);
        Files.createFile(subdir.resolve("file1.txt"));
        Files.createFile(tempDir.resolve("file2.txt"));

        FileTreeCrawler crawler = new FileTreeCrawler(new String[]{".txt"});
        Files.walkFileTree(tempDir, crawler);

        ArrayList<FileResult> results = crawler.getResults();
        assertEquals(2, results.size(), "Should find files in nested subdirectories");
    }

    @Test
    public void testCrawlerResultPathsMatchCreatedFiles() throws IOException {
        // Create files
        Path file1 = tempDir.resolve("file1.txt");
        Path file2 = tempDir.resolve("file2.txt");
        Files.createFile(file1);
        Files.createFile(file2);

        FileTreeCrawler crawler = new FileTreeCrawler(new String[]{".txt"});
        Files.walkFileTree(tempDir, crawler);

        ArrayList<FileResult> results = crawler.getResults();
        assertTrue(results.stream().anyMatch(r -> r.getPath().equals(file1)),
                "Should find file1.txt");
        assertTrue(results.stream().anyMatch(r -> r.getPath().equals(file2)),
                "Should find file2.txt");
    }
}
