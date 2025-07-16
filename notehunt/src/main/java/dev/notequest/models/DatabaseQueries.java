package dev.notequest.models;

public class DatabaseQueries {
    public final static String SETUP_SCHEMA= """
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
                                    'Error'
                                )),
                Last_Modified   DATETIME 
                                NOT NULL,
                Error_Message   VARCHAR
                );
            """;
    public class IndexingStatus {
        public final static String PENDING = "Pending";
        public final static String IN_PROGRESS = "In_Progress";
        public final static String COMPLETE = "Complete";
        public final static String ERROR = "Error";
    }

    public final static String UPDATE_CURRENT_FILE_STATUS = """
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
                AND vals.Last_Modified > fs.Last_Modified
                THEN UPDATE SET
                    Status        = CASE 
                                    WHEN vals.Status = 'Error' THEN 'Error'
                                    ELSE 'Pending'
                                    END,
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
                    CASE 
                    WHEN vals.Status = 'Error' THEN 'Error'
                    ELSE 'Pending'
                    END,
                    vals.Last_Modified,
                    vals.Error_Message
                );
            """;
}
