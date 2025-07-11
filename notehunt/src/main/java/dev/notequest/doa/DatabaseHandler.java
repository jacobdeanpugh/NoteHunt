package dev.notequest.doa;

import java.sql.*;


public class DatabaseHandler {
    private static final String CONNECTION_URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
    private static final String CONNECTION_USER = "sa";
    private static final String CONNECTION_PSWD = "";

    private Connection conn;

    public DatabaseHandler() {
        try {
            this.conn = DriverManager.getConnection(CONNECTION_URL, CONNECTION_USER, CONNECTION_PSWD);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to establish connection to databse: ", e);
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
