package dev.notequest.handler;

import java.sql.*;

import com.google.common.eventbus.Subscribe;

import dev.notequest.models.DatabaseQueries;
import dev.notequest.service.FileResult;
import dev.notequest.service.FileResult.FileStatus;
import dev.notequest.events.*;

import java.io.File;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;


/**
 * DatabaseHandler manages database operations for the NoteQuest application.
 * This class handles file indexing, database schema setup, and responds to file system events.
 * Uses H2 embedded database for storing file metadata and indexing status.
 * 
 * Key responsibilities:
 * - Initialize and maintain database connection
 * - Set up database schema on startup
 * - Process file system crawl events to sync database with directory state
 * - Handle individual file change events (create, modify, delete)
 * - Maintain file metadata including paths, hashes, and indexing status
 */
public class DatabaseHandler {
    // Database connection configuration constants
    private static final String CONNECTION_URL = "jdbc:h2:file:./data/db;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1";
    private static final String CONNECTION_USER = "sa";
    private static final String CONNECTION_PSWD = "";

    // Database connection instance - maintained for the lifetime of this handler
    private Connection conn;

    /**
     * Constructor initializes the database connection and sets up the schema.
     * Creates the data directory if it doesn't exist and establishes H2 database connection.
     * 
     * @throws RuntimeException if database connection fails or directory creation is denied
     */
    public DatabaseHandler() {
        try {
            // Ensures the folder for database exists - creates ./data directory if missing
            // This is necessary because H2 won't create the directory structure automatically
            new File("./data").mkdirs();
            
            // Establish connection to H2 database with embedded mode configuration:
            // - AUTO_SERVER=TRUE allows multiple connections to the same database
            // - DB_CLOSE_DELAY=-1 keeps the database open until JVM shutdown
            this.conn = DriverManager.getConnection(CONNECTION_URL, CONNECTION_USER, CONNECTION_PSWD);
            
            // Initialize database schema (tables, indexes, etc.)
            setupSchema();
        } catch (SQLException e) {
            // Wrap SQL exceptions in runtime exception to simplify error handling
            throw new RuntimeException("Unable to establish connection to databse: ", e);
        } catch (SecurityException e) {
            // Handle case where file system permissions prevent directory creation
            throw new RuntimeException("Unable to create folder due to permissions", e);
        }
    }

    /**
     * Sets up the database schema by executing the schema creation SQL.
     * This method runs the initial database setup queries to create tables and indexes.
     * Should be idempotent - safe to run multiple times without causing errors.
     * 
     * @throws RuntimeException if schema setup SQL execution fails
     */
    private void setupSchema() {
        try(Statement stmt = conn.createStatement()) {
            // Execute schema setup SQL from DatabaseQueries constants
            // Using try-with-resources ensures Statement is properly closed
            stmt.execute(DatabaseQueries.SETUP_SCHEMA);
        } catch (SQLException e) {
            // Wrap SQL exception to maintain consistent error handling pattern
            throw new RuntimeException("Error occured setting up schema", e);
        }
    }

    /**
     * Tests the database connection validity.
     * Prints connection status to console for debugging purposes.
     * This is primarily used for troubleshooting database connectivity issues.
     * 
     * @throws RuntimeException if connection validity check fails
     */
    public void testDatabaseConnection() {
        try {
            // Check if connection is valid with 0 second timeout
            // isValid(0) performs a simple connectivity test without waiting
            System.out.println(this.conn.isValid(0) ? "Connection Valid" : "Connection Not Valid");
        } catch (SQLException e) {
            throw new RuntimeException("Unable to establish connection to databse: ", e);
        }
    }

    /**
     * Event handler for FileTreeCrawledEvent.
     * This method is automatically called by the EventBus when a directory crawl completes.
     * 
     * Processes file system crawl results by:
     * 1. Updating database with all files found in the current directory scan
     * 2. Identifying files that exist in database but are no longer in the directory
     * 3. Marking those missing files as deleted to maintain database consistency
     * 
     * @param event FileTreeCrawledEvent containing the results of directory crawling
     */
    @Subscribe
    public void handleFileTreeCrawledEvent(FileTreeCrawledEvent event) {
        // Get the list of files found during directory crawling
        FileResult[] eventResults = event.getFileResults();

        // Insert or update current files in the database
        // This ensures all currently existing files are properly tracked
        mergeFilesIntoTable(eventResults);

        // Find files that exist in database but not in current directory scan
        // These are files that have been deleted since the last crawl
        flagStaleFilesInDirectory(eventResults);
    
    }

    /**
     * Identifies files that exist in the database but are missing from the current directory scan.
     * Uses SQL array operations to efficiently find the difference between database records
     * and current directory contents.
     * 
     * This method performs a set difference operation: (database files) - (current directory files)
     * 
     * @param currentDirectoryFiles List of FileResult objects from current directory scan
     * @return ArrayList of file path hashes for files that should be removed from database
     */
    private void flagStaleFilesInDirectory(FileResult[] currentDirectoryFiles) {
        // List to store file path hashes of files that have been deleted
        
        // Convert FileResult objects to array of file path hashes for SQL operation
        // We use hashes instead of full paths for efficiency and consistency
        String[] currentDirectoryFilesPathHashes = new String[currentDirectoryFiles.length];

        // Extract hash values from each FileResult object
        for(int i = 0; i < currentDirectoryFiles.length; i++) {
            currentDirectoryFilesPathHashes[i] = currentDirectoryFiles[i].getFilePathHash();
        }

        try (PreparedStatement ps = conn.prepareStatement(DatabaseQueries.FLAG_STALE_FILES_IN_DIRECTORY)) {
            // Create SQL array from file path hashes for database query
            // This allows us to pass the entire array as a single parameter
            Array SQLCompatibleArray = conn.createArrayOf("VARCHAR", currentDirectoryFilesPathHashes);

            // Set the array parameter in prepared statement (first and only parameter)
            ps.setArray(1, SQLCompatibleArray);

            // Execute query to find files in database but not in current directory
            // The query should return files that exist in DB but not in the provided array
            ps.execute();

        } catch (SQLException e) {
            // Wrap SQL exception for consistent error handling
            throw new RuntimeException("An unexpected error occured getting current directory file difference", e);
        }
    }

    /**
     * Inserts or updates file records in the database using batch processing.
     * This method performs an "upsert" operation - insert if new, update if exists.
     * 
     * Updates file metadata including:
     * - File path and hash for identification
     * - Indexing status (PENDING for successful files, ERROR for problematic ones)
     * - Last modified timestamp for change detection
     * - Exception messages for debugging failed files
     * 
     * Uses batch processing for better performance when handling many files.
     * 
     * @param currentDirectoryFiles List of FileResult objects to insert/update in database
     */
    private void mergeFilesIntoTable(FileResult... fileResults) {
        try (PreparedStatement ps = conn.prepareStatement(DatabaseQueries.STANDARD_MERGE_INTO_TABLE)) {
            // Process each file result and add to batch for efficient execution
            for (FileResult fr: fileResults) {
                // Convert FileResult's Instant to SQL Timestamp for database storage
                Timestamp last_modified_timestamp = new Timestamp(fr.getLastModified().toMillis());

                // Set parameters for prepared statement (index corresponds to SQL placeholders)
                ps.setString(1, fr.getPath().toString());           // File path as string
                ps.setString(2, fr.getFilePathHash());              // Hash for efficient lookups
                ps.setString(3, fr.getFileStatus().toString());     // Indexing status based on file processing result
                ps.setTimestamp(4, last_modified_timestamp);        // When file was last modified
                ps.setString(5, fr.getExc().getMessage());          // Exception message (null if no error)

                // Add current set of parameters to the batch
                ps.addBatch();
            }

            // Execute all batched statements at once for better performance
            // Returns array of update counts for each statement in the batch
            int[] counts = ps.executeBatch();                   

            // Log the number of records processed for monitoring
            System.out.println("Upserted " + counts.length + " rows.");

        } catch (SQLException e) {
            // Wrap SQL exception for consistent error handling
            throw new RuntimeException("An unexpected error occured updating current files status", e);
        }
    }

    /**
     * Event handler for FileChangeEvent.
     * This method is automatically called by the EventBus when individual file changes are detected.
     * 
     * Handles different types of file system events:
     * - ENTRY_CREATE: File was created (currently no action)
     * - ENTRY_MODIFY: File was modified (marks file as pending)
     * - ENTRY_DELETE: File was deleted (removes from database)
     * 
     * This provides real-time updates between directory crawls for immediate consistency.
     * 
     * @param event FileChangeEvent containing information about the changed file
     */
    @Subscribe
    public void handleFileChangeEvent(FileChangeEvent event) {
        // Handle different file system events based on the type of change
        mergeFilesIntoTable(event.getFileResult());
    }

    public ArrayList<FileResult> fetchPendingFiles() {
        ArrayList<FileResult> results  = new ArrayList<FileResult>();

        try(Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(DatabaseQueries.SELECT_PENDING_FILES);) {
                while (rs.next()) {
                    results.add(
                       new FileResult(
                            Paths.get(rs.getString("File_Path")),
                            FileStatus.getStatusFromString(rs.getString("Status")),
                            FileTime.from(rs.getTimestamp("Last_Modified").toInstant())
                        )
                    );
                }
        } catch (SQLException e) { 
            e.printStackTrace();
        }

        return results;
    }

    @Subscribe
    public void handlePendingFilesRequest(PendingFilesRequestEvent event) {
        System.out.println("Database Handler: Received request for pending files");

        try {
            ArrayList<FileResult> pendingFiles = fetchPendingFiles();

            event.getFuture().complete(pendingFiles);
        } catch (Exception e) {
            event.getFuture().completeExceptionally(e);
        }
    }

}