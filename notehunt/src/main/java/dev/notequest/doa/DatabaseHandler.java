package dev.notequest.doa;

import dev.notequest.doa.models.DatabaseQueries;

import java.sql.*;
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

}
