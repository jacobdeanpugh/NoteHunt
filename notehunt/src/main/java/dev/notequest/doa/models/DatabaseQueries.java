package dev.notequest.doa.models;

public class DatabaseQueries {
    public final static String SETUP_SCHEMA= """
            CREATE TABLE IF NOT EXISTS file_states (
                File_Path       VARCHAR(1024) 
                                NOT NULL,
                File_Path_Hash  VARCHAR(32)
                                NOT NULL,
                Status          VARCHAR(15) 
                                NOT NULL
                                CHECK (Status IN (
                                    'Pending',
                                    'In_Progress',
                                    'Complete'
                                )),
                Last_Modified   DATETIME 
                                NOT NULL
                );
            """;
}
