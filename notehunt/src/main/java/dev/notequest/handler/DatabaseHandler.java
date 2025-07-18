package dev.notequest.handler;

import java.sql.*;

import com.google.common.eventbus.Subscribe;

import dev.notequest.models.DatabaseQueries;
import dev.notequest.service.FileResult;
import dev.notequest.handler.events.*;
import java.util.ArrayList;

import java.io.File;


public class DatabaseHandler {
    private static final String CONNECTION_URL = "jdbc:h2:file:./data/db;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1";
    private static final String CONNECTION_USER = "sa";
    private static final String CONNECTION_PSWD = "";

    private Connection conn;

    public DatabaseHandler() {
        try {
            // Ensures the folder for database exists
            new File("./data").mkdirs();
            this.conn = DriverManager.getConnection(CONNECTION_URL, CONNECTION_USER, CONNECTION_PSWD);
            this.conn.setAutoCommit(false);
            setupSchema();
        } catch (SQLException e) {
            throw new RuntimeException("Unable to establish connection to databse: ", e);
        } catch (SecurityException e) {
            throw new RuntimeException("Unable to create folder due to permissions", e);
        }
    }

    private void setupSchema() {
        try(Statement stmt = conn.createStatement()) {
            stmt.execute(DatabaseQueries.SETUP_SCHEMA);
        } catch (SQLException e) {
            throw new RuntimeException("Error occured setting up schema", e);
        }
    }

    public void testDatabaseConnection() {
        try {
            System.out.println(this.conn.isValid(0) ? "Connection Valid" : "Connection Not Valid");
        } catch (SQLException e) {
            throw new RuntimeException("Unable to establish connection to databse: ", e);
        }
    }

    @Subscribe
    public void handleFileTreeCrawledEvent(FileTreeCrawledEvent event) {
        System.out.println(getDirectoryFilesDiff(event.getFileResults()));

        /*try (PreparedStatement ps = conn.prepareStatement(DatabaseQueries.UPDATE_CURRENT_FILE_STATUS)) {
            for (FileResult fr: event.getFileResults()) {
                Timestamp last_modified_timestamp = new Timestamp(fr.getLastModified().toMillis());

                ps.setString(1, fr.getPath().toString());
                ps.setString(2, fr.getFilePathHash());
                ps.setString(3,
                    fr.getFileStatus() == FileResult.FileStatus.SUCCESS ?
                    DatabaseQueries.IndexingStatus.PENDING : DatabaseQueries.IndexingStatus.ERROR);
                ps.setTimestamp(4, last_modified_timestamp);
                ps.setString(5, fr.getExc().getMessage());

                ps.addBatch();
            }

            int[] counts = ps.executeBatch();  // sends *all* rows in one go
            conn.commit();                     

            System.out.println("Upserted " + counts.length + " rows.");

        } catch (SQLException e) {
            throw new RuntimeException("An unexpected error occured updating current files status", e);
        } */
    }

    private ArrayList<String> getDirectoryFilesDiff(ArrayList<FileResult> currentDirectoryFiles) {
        ArrayList<String> diffFilePathHashes = new ArrayList<String>();
        
        String[] currentDirectoryFilesPathHashes = new String[currentDirectoryFiles.size()];

        for(int i = 0; i < currentDirectoryFiles.size(); i++) {
            currentDirectoryFilesPathHashes[i] = currentDirectoryFiles.get(i).getFilePathHash();
        }

        try (PreparedStatement ps = conn.prepareStatement(DatabaseQueries.GET_CURRENT_DIRECTORY_FILE_DIFF)) {
            Array SQLCompatibleArray = conn.createArrayOf("VARCHAR", currentDirectoryFilesPathHashes);

            ps.setArray(1, SQLCompatibleArray);

            try(ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    diffFilePathHashes.add(rs.getString("File_Path_Hash"));
                }
            }

            return diffFilePathHashes;

        } catch (SQLException e) {
            throw new RuntimeException("An unexpected error occured getting current directory file difference", e);
        }
    }

    @Subscribe
    public void handleFileChangeEvent(FileChangeEvent event) {
        System.out.println(event.getPath());
    }

}
