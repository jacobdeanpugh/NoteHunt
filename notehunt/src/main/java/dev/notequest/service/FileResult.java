package dev.notequest.service;

import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import dev.notequest.util.MD5Util;

/**
 * FileResult encapsulates the results from visiting a single file during tree traversal.
 * Each FileResult represents one file that was encountered, whether successfully
 * processed or failed due to errors.
 * 
 * This class serves as a data transfer object to communicate file information
 * back to the calling code, including error details when file access fails.
 */

public class FileResult {
        
        /**
         * Enum representing the outcome of a file visit operation.
         * Used to distinguish between successful file processing and error conditions.
         */
        public enum FileStatus {
            /** File was successfully visited and processed without errors */
            SUCCESS,
            /** An error occurred while attempting to visit or process the file */
            ERROR
        }
        
        private Path filePath;          // The absolute or relative path to the file
        private String filePathHash;
        private FileStatus fileStatus;  // Success or error status of the file visit
        private FileTime lastModified;  // Last modification timestamp (null for error cases)
        private Exception exc;          // Exception details when visitFileFailed is called
        
        /**
         * Constructor for successful file visits.
         * Creates a FileResult with SUCCESS status and file metadata.
         * 
         * @param filePath The path of the successfully visited file
         * @param fileStatus The status of the file visit (should be SUCCESS)
         * @param lastModified The last modified timestamp of the file
         */
        public FileResult(Path filePath, FileStatus fileStatus, FileTime lastModified) {
            this.filePath = filePath;
            this.filePathHash = MD5Util.md5Hex(filePath.toString());
            this.fileStatus = fileStatus;
            this.lastModified = lastModified;
        }

        /**
         * Constructor for failed file visits.
         * Creates a FileResult with ERROR status and exception details.
         * 
         * @param filePath The path of the file that failed to be visited
         * @param fileStatus The status of the file visit (should be ERROR)
         * @param exc The exception that occurred during the failed visit attempt
         */
        public FileResult(Path filePath, FileStatus fileStatus, Exception exc) {
            this.filePath = filePath;
            this.filePathHash = MD5Util.md5Hex(filePath.toString());
            this.fileStatus = fileStatus;
            this.exc = exc;
        }
        
        /**
         * Gets the file path associated with this result.
         * @return The Path object representing the file location
         */
        public Path getPath() { 
            return this.filePath; 
        }

        public String getFilePathHash() {
            return this.filePathHash;
        }
        
        /**
         * Gets the status of the file visit operation.
         * @return FileStatus indicating SUCCESS or ERROR
         */
        public FileStatus getFileStatus() { 
            return this.fileStatus; 
        }
        
        /**
         * Gets the last modified timestamp of the file.
         * @return FileTime representing when the file was last modified, or null for error cases
         */
        public FileTime getLastModified() { 
            return this.lastModified; 
        }
        
        /**
         * Gets the exception that occurred during a failed file visit.
         * @return Exception object containing error details, or null for successful visits
         */
        public Exception getExc() {
            if (this.exc == null) {
                // e.g. represent “no error” with a harmless exception
                return new Exception("No exception recorded");
            }
        return this.exc;
}

        
        /**
         * Returns a string representation of the FileResult for debugging and logging.
         * Format: "filePath fileStatus lastModified"
         * 
         * @return String representation of this FileResult
         */
        @Override
        public String toString() {
            return this.filePath + " " + this.fileStatus + " " + this.lastModified + " " + this.filePathHash;
        }
    }