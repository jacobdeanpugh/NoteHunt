package dev.notequest.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

import dev.notequest.service.FileResult.FileStatus;

public class FileResultTest {

    @Test
    public void testSuccessConstructorSetsFields() {
        // Test success constructor initializes fields correctly
        var path = Paths.get("/test/file.txt");
        var status = FileStatus.PENDING;
        var lastModified = FileTime.from(Instant.now());

        FileResult result = new FileResult(path, status, lastModified);

        assertEquals(path, result.getPath(), "Path should match constructor argument");
        assertEquals(status, result.getFileStatus(), "Status should match constructor argument");
        assertEquals(lastModified, result.getLastModified(), "Last modified should match constructor argument");
    }

    @Test
    public void testGetExcReturnsDefaultWhenNoError() {
        // Test that getExc() returns sentinel exception when no error
        var result = new FileResult(Paths.get("/test/file.txt"), FileStatus.COMPLETE, FileTime.from(Instant.now()));
        Exception exc = result.getExc();

        assertNotNull(exc, "getExc() should never return null");
        assertTrue(exc.getMessage().contains("No exception"), "getExc() should return sentinel message when no error");
    }

    @Test
    public void testErrorConstructorSetsException() {
        // Test error constructor sets path, status, and exception
        var path = Paths.get("/test/file.txt");
        var exception = new RuntimeException("File not found");

        FileResult result = new FileResult(path, FileStatus.ERROR, exception);

        assertEquals(path, result.getPath(), "Path should match constructor argument");
        assertEquals(FileStatus.ERROR, result.getFileStatus(), "Status should be ERROR");
        assertEquals(exception, result.getExc(), "Exception should match constructor argument");
    }

    @Test
    public void testGetFilePathHashIsConsistent() {
        // Test that getFilePathHash() is consistent for same path
        var path = Paths.get("/test/file.txt");
        FileResult result1 = new FileResult(path, FileStatus.PENDING, FileTime.from(Instant.now()));
        FileResult result2 = new FileResult(path, FileStatus.PENDING, FileTime.from(Instant.now()));

        assertEquals(result1.getFilePathHash(), result2.getFilePathHash(),
                "Same path should produce same hash");
    }

    @Test
    public void testFileStatusGetStatusFromString() {
        // Test FileStatus.getStatusFromString() for all values
        assertEquals(FileStatus.PENDING, FileStatus.getStatusFromString("Pending"));
        assertEquals(FileStatus.IN_PROGRESS, FileStatus.getStatusFromString("In_Progress"));
        assertEquals(FileStatus.COMPLETE, FileStatus.getStatusFromString("Complete"));
        assertEquals(FileStatus.ERROR, FileStatus.getStatusFromString("Error"));
        assertEquals(FileStatus.DELETED, FileStatus.getStatusFromString("Deleted"));
    }

    @Test
    public void testFileStatusRoundTrip() {
        // Test that status can be converted to string and back
        for (FileStatus status : FileStatus.values()) {
            String statusStr = status.toString();
            FileStatus recovered = FileStatus.getStatusFromString(statusStr);
            assertEquals(status, recovered, "Status should round-trip through string conversion");
        }
    }

    @Test
    public void testFileStatusToString() {
        // Test FileStatus.toString() returns expected strings
        assertEquals("Pending", FileStatus.PENDING.toString());
        assertEquals("In_Progress", FileStatus.IN_PROGRESS.toString());
        assertEquals("Complete", FileStatus.COMPLETE.toString());
        assertEquals("Error", FileStatus.ERROR.toString());
        assertEquals("Deleted", FileStatus.DELETED.toString());
    }
}
