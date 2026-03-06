package dev.notequest.handler;

import java.sql.*;

import com.google.common.eventbus.Subscribe;
import com.zaxxer.hikari.HikariDataSource;

import dev.notequest.models.DatabaseQueries;
import dev.notequest.provider.ConnectionPoolProvider;
import dev.notequest.service.FileResult;
import dev.notequest.service.FileResult.FileStatus;
import dev.notequest.events.*;

import java.io.File;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


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
    // Connection pool for thread-safe database access
    private HikariDataSource dataSource;

    // Test-only connection for injected test doubles
    private Connection testConnection;

    /**
     * Constructor initializes the database connection pool and sets up the schema.
     * Creates the data directory if it doesn't exist and gets HikariCP connection pool.
     *
     * @throws RuntimeException if database connection fails or directory creation is denied
     */
    public DatabaseHandler() {
        try {
            // Ensures the folder for database exists - creates ./data directory if missing
            // This is necessary because H2 won't create the directory structure automatically
            new File("./data").mkdirs();

            // Get the singleton HikariCP connection pool for thread-safe database access
            // The pool manages connections efficiently and handles concurrent requests
            this.dataSource = ConnectionPoolProvider.getInstance();

            // Initialize database schema (tables, indexes, etc.)
            setupSchema();
        } catch (SecurityException e) {
            // Handle case where file system permissions prevent directory creation
            throw new RuntimeException("Unable to create folder due to permissions", e);
        }
    }

    /**
     * Constructor for testing - accepts an external database connection.
     * This allows tests to provide an in-memory H2 database or other test double.
     *
     * @param conn Database connection for testing
     * @throws RuntimeException if schema setup fails
     */
    public DatabaseHandler(Connection conn) {
        // Test-only: store connection and skip pool initialization
        this.dataSource = null;
        this.testConnection = conn;
        setupSchema(conn);
    }

    /**
     * Gets a database connection from either the connection pool or test connection.
     * Used internally by all database operations to support both production and test modes.
     *
     * @return Connection from pool (production) or test connection (testing)
     * @throws SQLException if connection retrieval fails
     */
    private Connection getConnection() throws SQLException {
        if (testConnection != null) {
            return testConnection;
        }
        return dataSource.getConnection();
    }

    /**
     * Sets up the database schema by executing the schema creation SQL.
     * Called during no-arg constructor to initialize pool and schema.
     * Acquires a fresh connection from the pool for schema setup.
     *
     * @throws RuntimeException if schema setup SQL execution fails
     */
    private void setupSchema() {
        Connection conn = null;
        try {
            conn = getConnection();
            setupSchema(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Error occurred setting up schema", e);
        } finally {
            // Only close connection if it came from the pool (not test connection)
            if (testConnection == null && conn != null) {
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Sets up the database schema using the provided connection.
     * Called by test constructor or internally from setupSchema().
     *
     * @param conn Database connection to use for schema setup
     * @throws RuntimeException if schema setup SQL execution fails
     */
    private void setupSchema(Connection conn) {
        try(Statement stmt = conn.createStatement()) {
            // Execute schema setup SQL from DatabaseQueries constants
            // Using try-with-resources ensures Statement is properly closed
            stmt.execute(DatabaseQueries.SETUP_SCHEMA);
        } catch (SQLException e) {
            // Wrap SQL exception to maintain consistent error handling pattern
            throw new RuntimeException("Error occurred setting up schema", e);
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
        Connection conn = null;
        try {
            // Check if connection is valid with 0 second timeout
            // isValid(0) performs a simple connectivity test without waiting
            conn = getConnection();
            System.out.println(conn.isValid(0) ? "Connection Valid" : "Connection Not Valid");
        } catch (SQLException e) {
            throw new RuntimeException("Unable to establish connection to database: ", e);
        } finally {
            // Only close connection if it came from the pool (not test connection)
            if (testConnection == null && conn != null) {
                try { conn.close(); } catch (SQLException ignored) {}
            }
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

        Connection conn = null;
        try {
            conn = getConnection();
            try (PreparedStatement ps = conn.prepareStatement(DatabaseQueries.FLAG_STALE_FILES_IN_DIRECTORY)) {
                // Create SQL array from file path hashes for database query
                // This allows us to pass the entire array as a single parameter
                Array SQLCompatibleArray = conn.createArrayOf("VARCHAR", currentDirectoryFilesPathHashes);

                // Set the array parameter in prepared statement (first and only parameter)
                ps.setArray(1, SQLCompatibleArray);

                // Execute query to find files in database but not in current directory
                // The query should return files that exist in DB but not in the provided array
                ps.execute();
            }
        } catch (SQLException e) {
            // Wrap SQL exception for consistent error handling
            throw new RuntimeException("An unexpected error occured getting current directory file difference", e);
        } finally {
            // Only close connection if it came from the pool (not test connection)
            if (testConnection == null && conn != null) {
                try { conn.close(); } catch (SQLException ignored) {}
            }
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
    void mergeFilesIntoTable(FileResult... fileResults) {
        Connection conn = null;
        try {
            conn = getConnection();
            try (PreparedStatement ps = conn.prepareStatement(DatabaseQueries.STANDARD_MERGE_INTO_TABLE)) {
                // Process each file result and add to batch for efficient execution
                for (FileResult fr: fileResults) {
                    // Convert FileResult's Instant to SQL Timestamp for database storage
                    // Handle null lastModified for error cases by using current time
                    Timestamp last_modified_timestamp = fr.getLastModified() != null
                        ? new Timestamp(fr.getLastModified().toMillis())
                        : new Timestamp(System.currentTimeMillis());

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
            }
        } catch (SQLException e) {
            // Wrap SQL exception for consistent error handling
            throw new RuntimeException("An unexpected error occured updating current files status", e);
        } finally {
            // Only close connection if it came from the pool (not test connection)
            if (testConnection == null && conn != null) {
                try { conn.close(); } catch (SQLException ignored) {}
            }
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

        Connection conn = null;
        try {
            conn = getConnection();
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
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching pending files", e);
        } finally {
            // Only close connection if it came from the pool (not test connection)
            if (testConnection == null && conn != null) {
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }

        return results;
    }

    @Subscribe
    public void handlePendingFilesRequest(PendingFilesRequestEvent event) {
        try {
            ArrayList<FileResult> pendingFiles = fetchPendingFiles();

            event.getFuture().complete(pendingFiles);
        } catch (Exception e) {
            event.getFuture().completeExceptionally(e);
        }
    }

    @Subscribe
    public void handleSetFilesToCompleteEvent(SetFilesToCompleteEvent event) {
        mergeFilesIntoTable(event.getCompletedFiles());
    }

    /**
     * Retrieves counts of files grouped by status.
     * Returns a map with status names as keys and file counts as values.
     *
     * @return Map<String, Long> where keys are status values (e.g., "Pending", "Complete")
     *         and values are the counts of files with that status
     * @throws RuntimeException if the database query fails
     */
    public Map<String, Long> getStatusCounts() {
        Map<String, Long> counts = new HashMap<>();
        Connection conn = null;
        try {
            conn = getConnection();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(DatabaseQueries.COUNT_FILES_BY_STATUS)) {
                while (rs.next()) {
                    counts.put(rs.getString("Status"), rs.getLong("cnt"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching status counts", e);
        } finally {
            // Only close connection if it came from the pool (not test connection)
            if (testConnection == null && conn != null) {
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
        return counts;
    }

    /**
     * Retrieves the timestamp of the last completed file indexing operation.
     * Queries for the most recent Last_Modified timestamp among all files
     * with Complete status.
     *
     * @return LocalDateTime of the most recently completed file, or null if no
     *         completed files exist in the database
     * @throws RuntimeException if the database query fails
     */
    public LocalDateTime getLastSyncTime() {
        Connection conn = null;
        try {
            conn = getConnection();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(DatabaseQueries.GET_LAST_SYNC_TIME)) {
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp("last_sync");
                    if (ts != null) {
                        return ts.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching last sync time", e);
        } finally {
            // Only close connection if it came from the pool (not test connection)
            if (testConnection == null && conn != null) {
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
        return null;
    }

}