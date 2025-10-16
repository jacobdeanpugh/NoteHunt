package dev.notequest.models;

public class DatabaseQueries {
    // Constants for indexing status values
    public class IndexingStatus {
        public final static String PENDING = "Pending";
        public final static String IN_PROGRESS = "In_Progress";
        public final static String COMPLETE = "Complete";
        public final static String ERROR = "Error";
        public final static String DELETED = "Deleted";
    }

    // Schema setup - DDL operations
    public final static String SETUP_SCHEMA = """
            CREATE TABLE IF NOT EXISTS file_states (
                File_Path       VARCHAR(1024)
                                NOT NULL,
                File_Path_Hash  VARCHAR(32) PRIMARY Key
                                NOT NULL,
                Status          VARCHAR(15)
                                NOT NULL
                                CHECK (Status IN (
                                    'Pending',
                                    'In_Progress',
                                    'Complete',
                                    'Error',
                                    'Deleted'
                                )),
                Last_Modified   DATETIME
                                NOT NULL,
                Error_Message   VARCHAR
            );
            """;

    public final static String STANDARD_MERGE_INTO_TABLE = """
            MERGE INTO file_states AS fs
            USING (
                VALUES
                    (?, ?, ?, ?, ?)
            ) AS vals(
                File_Path,
                File_Path_Hash,
                Status,
                Last_Modified,
                Error_Message
            )
            ON fs.File_Path_Hash = vals.File_Path_Hash
            WHEN MATCHED
            AND (
                vals.Last_Modified > fs.Last_Modified OR
                (fs.Last_Modified = vals.Last_Modified AND vals.Status <> 'Pending')
            )
            THEN UPDATE SET
                Status        = vals.Status,
                Last_Modified = vals.Last_Modified,
                Error_Message = vals.Error_Message
            WHEN NOT MATCHED
            THEN INSERT (
                File_Path,
                File_Path_Hash,
                Status,
                Last_Modified,
                Error_Message
            )
            VALUES (
                vals.File_Path,
                vals.File_Path_Hash,
                vals.Status,
                vals.Last_Modified,
                vals.Error_Message
            );
            """;
    
    public final static String MARK_FILES_AS_DELETED = """
            UPDATE file_states
            SET
                Status = 'Deleted'
                
            WHERE
                File_Path_Hash = ANY(?);
            """;

    public final static String MARK_FILES_AS_PENDING = """
            UPDATE file_states
            SET
                status = 'Pending'
            WHERE
                (File_Path_Hash = ANY(?));
            """;

    // File operations - SELECT queries
    public final static String FLAG_STALE_FILES_IN_DIRECTORY = """
            UPDATE file_states
            SET status = 'Deleted'
            WHERE NOT (
                File_Path_Hash = Any(?)
            ) 
                AND Status <> 'Deleted';
            """;

    // File operations - DELETE queries
    public final static String REMOVE_FILES_FROM_TABLE = """
            DELETE FROM file_states
            WHERE (
                File_Path_Hash = ANY(?)
            );
            """;
    // SELECT Pending File
    public final static String SELECT_PENDING_FILES = """
            SELECT * FROM file_states
            WHERE (
                Status = 'Pending'
            )
            """;
}