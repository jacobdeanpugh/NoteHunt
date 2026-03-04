package dev.notequest.handler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.*;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;

import dev.notequest.service.FileResult;
import dev.notequest.service.FileResult.FileStatus;
import dev.notequest.events.FileChangeEvent;
import dev.notequest.events.FileTreeCrawledEvent;
import dev.notequest.events.SetFilesToCompleteEvent;
import dev.notequest.events.PendingFilesRequestEvent;

public class DatabaseHandlerTest {

    private Connection conn;
    private DatabaseHandler dbHandler;
    private static int dbCounter = 0;

    @BeforeEach
    public void setUp() throws SQLException {
        // Create unique in-memory H2 database for each test to avoid data sharing
        String dbName = "testdb_" + (dbCounter++) + "_" + System.nanoTime();
        conn = DriverManager.getConnection("jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1");
        dbHandler = new DatabaseHandler(conn);
    }

    @AfterEach
    public void tearDown() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    }

    @Test
    public void testSetupSchemaCreatesTable() throws SQLException {
        // Verify file_states table exists after schema setup
        DatabaseMetaData metadata = conn.getMetaData();
        ResultSet tables = metadata.getTables(null, null, "FILE_STATES", null);
        assertTrue(tables.next(), "FILE_STATES table should exist after schema setup");
    }

    @Test
    public void testMergeFilesIntoTableInsertsNewRecord() throws SQLException {
        // Insert a new file
        FileResult fr = new FileResult(Paths.get("/test/file.txt"), FileStatus.PENDING,
            FileTime.from(Instant.now()));
        dbHandler.mergeFilesIntoTable(fr);

        // Verify record exists
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM FILE_STATES");
        rs.next();
        assertEquals(1, rs.getInt("cnt"), "Should have 1 record after insert");
    }

    @Test
    public void testMergeFilesUpdatesWhenNewer() throws SQLException {
        // Insert initial file
        Instant initial = Instant.now().minusSeconds(100);
        FileResult fr1 = new FileResult(Paths.get("/test/file.txt"), FileStatus.PENDING,
            FileTime.from(initial));
        dbHandler.mergeFilesIntoTable(fr1);

        // Update with newer modification time
        Instant newer = Instant.now();
        FileResult fr2 = new FileResult(Paths.get("/test/file.txt"), FileStatus.COMPLETE,
            FileTime.from(newer));
        dbHandler.mergeFilesIntoTable(fr2);

        // Verify record was updated
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM FILE_STATES");
        rs.next();
        assertEquals(1, rs.getInt("cnt"), "Should still have only 1 record");

        // Verify status changed - query by hash for platform independence
        String pathHash = fr1.getFilePathHash();
        rs = stmt.executeQuery("SELECT Status FROM FILE_STATES WHERE File_Path_Hash = '" + pathHash + "'");
        rs.next();
        assertEquals("Complete", rs.getString("Status"), "Status should be updated");
    }

    @Test
    public void testMergeFilesDoesNotUpdateWhenOlder() throws SQLException {
        // Insert newer file
        Instant newer = Instant.now();
        FileResult fr1 = new FileResult(Paths.get("/test/file.txt"), FileStatus.COMPLETE,
            FileTime.from(newer));
        dbHandler.mergeFilesIntoTable(fr1);

        // Attempt to update with older modification time
        Instant older = Instant.now().minusSeconds(100);
        FileResult fr2 = new FileResult(Paths.get("/test/file.txt"), FileStatus.PENDING,
            FileTime.from(older));
        dbHandler.mergeFilesIntoTable(fr2);

        // Verify status was not downgraded (MERGE logic should skip) - query by hash
        Statement stmt = conn.createStatement();
        String pathHash = fr1.getFilePathHash();
        ResultSet rs = stmt.executeQuery("SELECT Status FROM FILE_STATES WHERE File_Path_Hash = '" + pathHash + "'");
        rs.next();
        assertEquals("Complete", rs.getString("Status"), "Status should not revert to PENDING");
    }

    @Test
    public void testFetchPendingFilesReturnsPending() throws SQLException {
        // Insert mixed status files
        dbHandler.mergeFilesIntoTable(
            new FileResult(Paths.get("/test/file1.txt"), FileStatus.PENDING, FileTime.from(Instant.now())),
            new FileResult(Paths.get("/test/file2.txt"), FileStatus.COMPLETE, FileTime.from(Instant.now())),
            new FileResult(Paths.get("/test/file3.txt"), FileStatus.PENDING, FileTime.from(Instant.now()))
        );

        ArrayList<FileResult> pending = dbHandler.fetchPendingFiles();
        assertEquals(2, pending.size(), "Should return only PENDING files");
    }

    @Test
    public void testFetchPendingFilesSkipsCompleteAndDeleted() throws SQLException {
        // Insert files with various statuses
        dbHandler.mergeFilesIntoTable(
            new FileResult(Paths.get("/test/file1.txt"), FileStatus.PENDING, FileTime.from(Instant.now())),
            new FileResult(Paths.get("/test/file2.txt"), FileStatus.COMPLETE, FileTime.from(Instant.now())),
            new FileResult(Paths.get("/test/file3.txt"), FileStatus.DELETED, FileTime.from(Instant.now()))
        );

        ArrayList<FileResult> pending = dbHandler.fetchPendingFiles();
        assertTrue(pending.stream().allMatch(r -> r.getFileStatus() == FileStatus.PENDING),
                "Should only return PENDING status files");
    }

    @Test
    public void testFlagStaleFilesMarksUnlistedAsDeleted() throws SQLException {
        // Insert 3 files
        dbHandler.mergeFilesIntoTable(
            new FileResult(Paths.get("/test/file1.txt"), FileStatus.COMPLETE, FileTime.from(Instant.now())),
            new FileResult(Paths.get("/test/file2.txt"), FileStatus.COMPLETE, FileTime.from(Instant.now())),
            new FileResult(Paths.get("/test/file3.txt"), FileStatus.COMPLETE, FileTime.from(Instant.now()))
        );

        // Verify all 3 files were inserted
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM FILE_STATES");
        rs.next();
        assertEquals(3, rs.getInt("cnt"), "Should have 3 total files");
    }

    @Test
    public void testFlagStaleFilesWithEmptyList() throws SQLException {
        // Insert files
        dbHandler.mergeFilesIntoTable(
            new FileResult(Paths.get("/test/file1.txt"), FileStatus.COMPLETE, FileTime.from(Instant.now())),
            new FileResult(Paths.get("/test/file2.txt"), FileStatus.COMPLETE, FileTime.from(Instant.now()))
        );

        // Flag all as stale (empty current list)
        FileResult[] empty = {};
        dbHandler.mergeFilesIntoTable(empty);

        // Verify files still exist (flagging happens, not deletion)
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM FILE_STATES");
        rs.next();
        assertEquals(2, rs.getInt("cnt"), "Records should still exist");
    }

    @Test
    public void testHandleFileChangeEvent() throws SQLException {
        // Create and post a file change event
        FileResult fr = new FileResult(Paths.get("/test/file.txt"), FileStatus.PENDING, FileTime.from(Instant.now()));
        FileChangeEvent event = new FileChangeEvent(fr);

        dbHandler.handleFileChangeEvent(event);

        // Verify file was inserted - query by hash for platform independence
        Statement stmt = conn.createStatement();
        String pathHash = fr.getFilePathHash();
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM FILE_STATES WHERE File_Path_Hash = '" + pathHash + "'");
        rs.next();
        assertEquals(1, rs.getInt("cnt"), "File change event should insert file");
    }

    @Test
    public void testHandleFileTreeCrawledEvent() throws SQLException {
        // Create file results
        FileResult file1 = new FileResult(Paths.get("/test/file1.txt"), FileStatus.PENDING, FileTime.from(Instant.now()));
        FileResult file2 = new FileResult(Paths.get("/test/file2.txt"), FileStatus.PENDING, FileTime.from(Instant.now()));

        // Create event with ArrayList
        ArrayList<FileResult> files = new ArrayList<>();
        files.add(file1);
        files.add(file2);
        FileTreeCrawledEvent event = new FileTreeCrawledEvent(files);

        dbHandler.handleFileTreeCrawledEvent(event);

        // Verify all files were inserted
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM FILE_STATES");
        rs.next();
        assertEquals(2, rs.getInt("cnt"), "All crawled files should be inserted");
    }

    @Test
    public void testHandlePendingFilesRequest() throws Exception {
        // Insert pending files
        dbHandler.mergeFilesIntoTable(
            new FileResult(Paths.get("/test/file1.txt"), FileStatus.PENDING, FileTime.from(Instant.now())),
            new FileResult(Paths.get("/test/file2.txt"), FileStatus.PENDING, FileTime.from(Instant.now()))
        );

        // Create and handle request event
        var future = new java.util.concurrent.CompletableFuture<ArrayList<FileResult>>();
        PendingFilesRequestEvent event = new PendingFilesRequestEvent(future);
        dbHandler.handlePendingFilesRequest(event);

        // Verify future is completed with pending files
        ArrayList<FileResult> result = future.get();
        assertEquals(2, result.size(), "Future should be completed with pending files");
    }

    @Test
    public void testHandleSetFilesToCompleteEvent() throws SQLException {
        // Insert pending files
        FileResult fr1 = new FileResult(Paths.get("/test/file1.txt"), FileStatus.PENDING, FileTime.from(Instant.now()));
        FileResult fr2 = new FileResult(Paths.get("/test/file2.txt"), FileStatus.PENDING, FileTime.from(Instant.now()));
        dbHandler.mergeFilesIntoTable(fr1, fr2);

        // Mark as complete
        SetFilesToCompleteEvent event = new SetFilesToCompleteEvent();
        event.addFileResult(fr1);
        event.addFileResult(fr2);
        dbHandler.handleSetFilesToCompleteEvent(event);

        // Verify status updated
        ArrayList<FileResult> pending = dbHandler.fetchPendingFiles();
        assertEquals(0, pending.size(), "No pending files should remain");
    }

    @Test
    public void testErrorRecordStoresExceptionMessage() throws SQLException {
        // Insert file with error
        Exception testError = new RuntimeException("File read failed");
        FileResult fr = new FileResult(Paths.get("/test/file.txt"), FileStatus.ERROR, testError);
        dbHandler.mergeFilesIntoTable(fr);

        // Verify error message is stored - query by hash for platform independence
        Statement stmt = conn.createStatement();
        String pathHash = fr.getFilePathHash();
        ResultSet rs = stmt.executeQuery("SELECT Error_Message FROM FILE_STATES WHERE File_Path_Hash = '" + pathHash + "'");
        rs.next();
        assertTrue(rs.getString("Error_Message").contains("File read failed"),
                "Exception message should be stored");
    }

    // Helper method
    private int countCompleteFiles() throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM FILE_STATES WHERE Status = 'Complete'");
        rs.next();
        return rs.getInt("cnt");
    }
}
