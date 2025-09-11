package dev.notequest.service;

import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.Path;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.util.ArrayList;
import dev.notequest.service.FileResult.FileStatus;

/**
 * FileTreeCrawler extends SimpleFileVisitor to traverse a file system tree
 * and collect information about files encountered during the traversal.
 * 
 * This class provides filtering capabilities based on file extensions and
 * records detailed information about each file including success/error status,
 * file paths, and modification times. It's designed to work with the NoteQuest
 * file monitoring system to catalog files within a directory structure.
 * 
 * The crawler can operate in two modes:
 * - Filtered mode: Only processes files with specified extensions
 * - Unfiltered mode: Processes all regular files in the directory tree
 * 
 * @author NoteQuest Development Team
 * @version 1.0
 * @since 1.0
 */
public class FileTreeCrawler extends SimpleFileVisitor<Path> {

    /**
     * FileExtension defines commonly used file extensions as constants.
     * This inner class provides a centralized location for managing supported
     * file types within the NoteQuest application.
     * 
     * Note: Extensions are case-sensitive and should include the leading dot.
     */
    public static class FileExtention {
        /** Plain text files */
        public final static String TXT = ".txt";
        /** Markdown files */
        public final static String MD = ".md";
        /** Portable Document Format files */
        public final static String PDF = ".pdf";
        /** Microsoft Word documents (corrected case) */
        public final static String DOCX = ".docx";
    }
    
    // Collection to store all file results encountered during tree traversal
    private ArrayList<FileResult> fileTreeCrawlerResults = new ArrayList<FileResult>();

    // Array of file extensions to filter by during traversal
    private String[] targetFileExtentions = {};

    /**
     * Constructor for filtered file tree crawling.
     * Creates a FileTreeCrawler that only processes files with the specified extensions.
     * 
     * @param targetFileExtentions Array of file extensions to include in the crawl.
     *                           Extensions should be from the class FileTreeCrawler.FileExtension
     */
    public FileTreeCrawler(String[] targetFileExtentions) {
        this.targetFileExtentions = targetFileExtentions;
    }
    
    /**
     * Default constructor for unfiltered file tree crawling.
     * Creates a FileTreeCrawler that processes all regular files encountered
     * during directory traversal without any extension filtering.
     */
    public FileTreeCrawler() {}

    /**
     * Determines if a file should be included based on extension filtering.
     * 
     * This method checks if the file's extension matches any of the target extensions
     * specified in the constructor. If no target extensions are specified, all files
     * are considered valid (no filtering applied).
     * 
     * @param file The file path to check against the extension filter
     * @return true if the file should be processed, false if it should be skipped
     */
    private Boolean fileIsInExtensionFilter(Path file) {
        // If no extensions are specified, include all files (no filtering)
        if (this.targetFileExtentions.length < 1) {
            return true;
        }

        // Check if the file ends with any of the target extensions
        for (String ext : this.targetFileExtentions) {
            if (file.toString().endsWith(ext)) {
                return true;
            }
        }

        // File doesn't match any target extension
        return false;
    }
   
    /**
     * Called for each file encountered during directory traversal.
     * 
     * This method is automatically invoked by the file visitor framework for every
     * file found in the directory tree. It applies extension filtering and creates
     * FileResult objects for files that match the criteria.
     * 
     * Only regular files (not directories, symbolic links, or special files) that
     * pass the extension filter are processed and added to the results collection.
     * 
     * @param file The path to the file being visited
     * @param attr Basic file attributes including size, timestamps, and file type
     * @return FileVisitResult.CONTINUE to continue traversal to the next file
     */
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
        // Only process regular files that match the extension filter
        if (attr.isRegularFile() && fileIsInExtensionFilter(file)) {
            // Create a successful result with the file path and metadata
            FileResult result = new FileResult(file, FileStatus.PENDING, attr.lastModifiedTime());
            fileTreeCrawlerResults.add(result);
        }
        // Continue visiting other files in the tree
        return FileVisitResult.CONTINUE;
    }
    
    /**
     * Called when a file cannot be visited due to an error.
     * 
     * This method is automatically invoked by the file visitor framework when
     * file access fails due to permissions, I/O errors, or other system-level issues.
     * 
     * The method creates an error FileResult to maintain a record of the failed
     * access attempt, which can be useful for debugging and error reporting.
     * 
     * @param file The path to the file that failed to be visited
     * @param exc The IOException that occurred during the visit attempt
     * @return FileVisitResult.CONTINUE to continue traversal despite the error
     * @throws IOException Re-throws the exception if needed for error handling
     */
    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        // Only record error for files that would have been processed
        if (fileIsInExtensionFilter(file)) {
            // Create an error result with exception details
            FileResult result = new FileResult(file, FileStatus.ERROR, exc);
            fileTreeCrawlerResults.add(result);
        }
        // Continue processing other files even if this one failed
        return FileVisitResult.CONTINUE;
    }
    
    /**
     * Returns the complete list of file results collected during tree traversal.
     * 
     * This method should be called after the file tree walking operation is complete
     * to retrieve all encountered files and their processing results. The returned
     * list includes both successfully processed files and files that encountered errors.
     * 
     * @return ArrayList containing FileResult objects for all encountered files
     *         that matched the extension filter criteria
     */
    public ArrayList<FileResult> getResults() {
        return fileTreeCrawlerResults;
    }
}