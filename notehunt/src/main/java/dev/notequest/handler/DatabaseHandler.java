package dev.notequest.handler;

import java.sql.*;

import com.google.common.eventbus.Subscribe;

import dev.notequest.models.DatabaseQueries;
import dev.notequest.service.FileResult;
import dev.notequest.handler.events.*;
import java.util.ArrayList;

import java.io.File;

/**
 * DatabaseHandler manages database operations for the NoteQuest application.
 * This class handles file indexing, database schema setup, and responds to file system events.
 * Uses H2 embedded database for storing file metadata and indexing status.
 */
public class DatabaseHandler {
    // Database connection configuration constants
    private static final String CONNECTION_URL = "jdbc:h2:file:./data/db;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1";
    private static final String CONNECTION_USER = "sa";
    private static final String CONNECTION_PSWD = "";

    // Database connection instance
    private Connection conn;

    /**
     * Constructor initializes the database connection and sets up the schema.
     * Creates the data directory if it doesn't exist and establishes H2 database connection.
     */
    public DatabaseHandler() {
        try {
            // Ensures the folder for database exists - creates ./data directory if missing
            new File("./data").mkdirs();
            
            // Establish connection to H2 database
            this.conn = DriverManager.getConnection(CONNECTION_URL, CONNECTION_USER, CONNECTION_PSWD);
            
            // Initialize database schema (tables, indexes, etc.)
            setupSchema();
        } catch (SQLException e) {
            throw new RuntimeException("Unable to establish connection to databse: ", e);
        } catch (SecurityException e) {
            throw new RuntimeException("Unable to create folder due to permissions", e);
        }
    }

    /**
     * Sets up the database schema by executing the schema creation SQL.
     * This method runs the initial database setup queries to create tables and indexes.
     */
    private void setupSchema() {
        try(Statement stmt = conn.createStatement()) {
            // Execute schema setup SQL from DatabaseQueries constants
            stmt.execute(DatabaseQueries.SETUP_SCHEMA);
        } catch (SQLException e) {
            throw new RuntimeException("Error occured setting up schema", e);
        }
    }

    /**
     * Tests the database connection validity.
     * Prints connection status to console for debugging purposes.
     */
    public void testDatabaseConnection() {
        try {
            // Check if connection is valid with 0 second timeout
            System.out.println(this.conn.isValid(0) ? "Connection Valid" : "Connection Not Valid");
        } catch (SQLException e) {
            throw new RuntimeException("Unable to establish connection to databse: ", e);
        }
    }

    /**
     * Event handler for FileTreeCrawledEvent.
     * Processes file system crawl results by updating database with current files
     * and removing files that no longer exist in the directory.
     * 
     * @param event FileTreeCrawledEvent containing the results of directory crawling
     */
    @Subscribe
    public void handleFileTreeCrawledEvent(FileTreeCrawledEvent event) {
        // Get the list of files found during directory crawling
        ArrayList<FileResult> eventResults = event.getFileResults();

        // Insert or update current files in the database
        insertCurrentFilesInDirectory(eventResults);

        // Find files that exist in database but not in current directory scan
        ArrayList<String> deletedFilesInDirectory = getDeletedFilesInDirectory(eventResults);
    
        // Remove deleted files from database
        removeFiles(deletedFilesInDirectory.toArray(new String[0]));
    }

    /**
     * Identifies files that exist in the database but are missing from the current directory scan.
     * Uses SQL array operations to efficiently find the difference between database records
     * and current directory contents.
     * 
     * @param currentDirectoryFiles List of FileResult objects from current directory scan
     * @return ArrayList of file path hashes for files that should be removed from database
     */
    private ArrayList<String> getDeletedFilesInDirectory(ArrayList<FileResult> currentDirectoryFiles) {
        // List to store file path hashes of deleted files
        ArrayList<String> diffFilePathHashes = new ArrayList<String>();
        
        // Convert FileResult objects to array of file path hashes
        String[] currentDirectoryFilesPathHashes = new String[currentDirectoryFiles.size()];

        for(int i = 0; i < currentDirectoryFiles.size(); i++) {
            currentDirectoryFilesPathHashes[i] = currentDirectoryFiles.get(i).getFilePathHash();
        }

        try (PreparedStatement ps = conn.prepareStatement(DatabaseQueries.GET_CURRENT_DIRECTORY_FILE_DIFF)) {
            // Create SQL array from file path hashes for database query
            Array SQLCompatibleArray = conn.createArrayOf("VARCHAR", currentDirectoryFilesPathHashes);

            // Set the array parameter in prepared statement
            ps.setArray(1, SQLCompatibleArray);

            // Execute query to find files in database but not in current directory
            try(ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Add each deleted file's path hash to result list
                    diffFilePathHashes.add(rs.getString("File_Path_Hash"));
                }
            }

            return diffFilePathHashes;

        } catch (SQLException e) {
            throw new RuntimeException("An unexpected error occured getting current directory file difference", e);
        }
    }

    /**
     * Inserts or updates file records in the database using batch processing.
     * Updates file metadata including path, hash, indexing status, and last modified timestamp.
     * 
     * @param currentDirectoryFiles List of FileResult objects to insert/update in database
     */
    private void insertCurrentFilesInDirectory(ArrayList<FileResult> currentDirectoryFiles) {
        try (PreparedStatement ps = conn.prepareStatement(DatabaseQueries.UPDATE_CURRENT_FILE_STATUS)) {
            // Process each file result and add to batch
            for (FileResult fr: currentDirectoryFiles) {
                // Convert last modified time to SQL timestamp
                Timestamp last_modified_timestamp = new Timestamp(fr.getLastModified().toMillis());

                // Set parameters for prepared statement
                ps.setString(1, fr.getPath().toString());           // File path
                ps.setString(2, fr.getFilePathHash());              // File path hash
                ps.setString(3,                                     // Indexing status based on file status
                    fr.getFileStatus() == FileResult.FileStatus.SUCCESS ?
                    DatabaseQueries.IndexingStatus.PENDING : DatabaseQueries.IndexingStatus.ERROR);
                ps.setTimestamp(4, last_modified_timestamp);        // Last modified timestamp
                ps.setString(5, fr.getExc().getMessage());          // Exception message (if any)

                // Add current parameters to batch
                ps.addBatch();
            }

            // Execute all batched statements at once for better performance
            int[] counts = ps.executeBatch();                   

            System.out.println("Upserted " + counts.length + " rows.");

        } catch (SQLException e) {
            throw new RuntimeException("An unexpected error occured updating current files status", e);
        }
    }

    /**
     * Removes files from the database using their file path hashes.
     * Uses SQL array operations for efficient bulk deletion.
     * 
     * @param filePathHashes List of file path hashes to remove from database
     */
    private void removeFiles(String... filePathHashes) {
        // Convert ArrayList to array for SQL array creation
        String[] rawFilePathHashes = filePathHashes;

        try (PreparedStatement ps = conn.prepareStatement(DatabaseQueries.REMOVE_FILES_FROM_TABLE)) {
            // Create SQL array from file path hashes
            Array SQLCompatibleArray = conn.createArrayOf("VARCHAR", rawFilePathHashes);

            // Set array parameter for deletion query
            ps.setArray(1, SQLCompatibleArray);

            // Execute deletion and get count of affected rows
            int count = ps.executeUpdate();

            System.out.println("Removed " + count + " rows.");

        } catch (SQLException e) {
            throw new RuntimeException("An unexpected error occured removing rows from database", e);
        }
    }

    /**
     * Event handler for FileChangeEvent.
     * Currently just logs the file path that changed.
     * This is a placeholder for future file change processing logic.
     * 
     * @param event FileChangeEvent containing information about the changed file
     */
    @Subscribe
    public void handleFileChangeEvent(FileChangeEvent event) {
        switch(event.getKind().name()) {
            case "ENTRY_CREATE" :
                break;

            case "ENTRY_MODIFY" : 
                break;

            case "ENTRY_DELETE" :
                removeFiles(event.getFilePathHash());
                break;
            default :
                break;
        }
    }
}
