package dev.notequest.provider;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Singleton provider for HikariCP connection pool.
 * Manages database connection pooling for thread-safe concurrent access.
 *
 * Uses double-checked locking for lazy initialization and thread safety.
 */
public class ConnectionPoolProvider {

    private static volatile HikariDataSource instance;
    private static final Object LOCK = new Object();

    /**
     * Gets or creates the singleton HikariDataSource.
     * Thread-safe lazy initialization using double-checked locking.
     *
     * @return HikariDataSource configured for H2 database
     */
    public static HikariDataSource getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = createDataSource();
                }
            }
        }
        return instance;
    }

    /**
     * Creates and configures HikariCP datasource.
     * Configures connection pool with:
     * - Maximum pool size: 10
     * - Minimum idle connections: 5
     * - H2 database with file-based persistence
     * - AUTO_SERVER enabled for concurrent access
     *
     * @return Configured HikariDataSource
     */
    private static HikariDataSource createDataSource() {
        HikariConfig config = new HikariConfig();

        // H2 database connection string
        // AUTO_SERVER=TRUE allows multiple connections from different processes
        // DB_CLOSE_DELAY=-1 keeps database open while connections exist
        config.setJdbcUrl("jdbc:h2:file:./data/db;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1");
        config.setUsername("sa");
        config.setPassword("");

        // Connection pool settings
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);      // 30 seconds
        config.setIdleTimeout(600000);            // 10 minutes
        config.setMaxLifetime(1800000);           // 30 minutes
        config.setPoolName("NoteHuntPool");
        config.setAutoCommit(true);

        return new HikariDataSource(config);
    }

    /**
     * Closes the singleton instance.
     * Used for cleanup during shutdown or testing.
     *
     * Thread-safe and idempotent - safe to call multiple times.
     */
    public static void closeInstance() {
        synchronized (LOCK) {
            if (instance != null && !instance.isClosed()) {
                instance.close();
                instance = null;
            }
        }
    }
}
