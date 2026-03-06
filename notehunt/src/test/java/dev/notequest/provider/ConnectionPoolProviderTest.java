package dev.notequest.provider;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConnectionPoolProvider singleton.
 * Verifies thread-safe lazy initialization, configuration, and cleanup.
 */
public class ConnectionPoolProviderTest {

    @BeforeEach
    public void setUp() {
        // Ensure clean state before each test
        ConnectionPoolProvider.closeInstance();
    }

    @AfterEach
    public void tearDown() {
        // Clean up after each test
        ConnectionPoolProvider.closeInstance();
    }

    /**
     * Test 1: getInstance() returns same object on repeated calls (singleton)
     */
    @Test
    public void testSingletonInstance() {
        HikariDataSource instance1 = ConnectionPoolProvider.getInstance();
        HikariDataSource instance2 = ConnectionPoolProvider.getInstance();

        assertNotNull(instance1, "First getInstance() should return non-null DataSource");
        assertNotNull(instance2, "Second getInstance() should return non-null DataSource");
        assertSame(instance1, instance2, "getInstance() should return same instance (singleton pattern)");
    }

    /**
     * Test 2: DataSource is configured with correct pool size
     */
    @Test
    public void testDataSourceIsConfigured() {
        HikariDataSource dataSource = ConnectionPoolProvider.getInstance();

        assertNotNull(dataSource, "DataSource should not be null");
        assertEquals(10, dataSource.getMaximumPoolSize(), "Maximum pool size should be 10");
        assertEquals(5, dataSource.getMinimumIdle(), "Minimum idle connections should be 5");
        assertEquals("NoteHuntPool", dataSource.getPoolName(), "Pool name should be 'NoteHuntPool'");
        assertTrue(dataSource.isAutoCommit(), "AutoCommit should be enabled");
    }

    /**
     * Test 3: Can successfully get a connection from the pool
     */
    @Test
    public void testCanGetConnection() throws Exception {
        HikariDataSource dataSource = ConnectionPoolProvider.getInstance();

        assertNotNull(dataSource, "DataSource should not be null");
        assertFalse(dataSource.isClosed(), "DataSource should not be closed");

        // Should be able to get a connection from the pool
        Connection connection = dataSource.getConnection();
        assertNotNull(connection, "Should be able to obtain a connection from pool");
        assertFalse(connection.isClosed(), "Connection should be open");

        // Clean up
        connection.close();
    }

    /**
     * Test 4: closeInstance() properly closes the DataSource
     */
    @Test
    public void testCloseInstance() throws Exception {
        HikariDataSource instance1 = ConnectionPoolProvider.getInstance();
        assertNotNull(instance1, "First instance should not be null");
        assertFalse(instance1.isClosed(), "DataSource should be open initially");

        // Close the instance
        ConnectionPoolProvider.closeInstance();

        // Verify it's closed
        assertTrue(instance1.isClosed(), "DataSource should be closed after closeInstance()");

        // Verify that calling getInstance() again creates a new instance
        HikariDataSource instance2 = ConnectionPoolProvider.getInstance();
        assertNotNull(instance2, "Second instance should not be null");
        assertNotSame(instance1, instance2, "After closeInstance(), getInstance() should create a new instance");
        assertFalse(instance2.isClosed(), "New instance should be open");
    }
}
