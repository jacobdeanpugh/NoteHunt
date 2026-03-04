package dev.notequest;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import dev.notequest.handler.EventBusRegistry;

/**
 * Basic smoke tests for the App.
 */
public class AppTest {

    @Test
    public void testEventBusRegistryIsNonNull() {
        // Sanity test that EventBusRegistry is properly initialized
        assertNotNull(EventBusRegistry.bus(), "EventBusRegistry.bus() should return non-null EventBus");
    }

    @Test
    public void testDatabaseSchemaSetupDoesNotThrow() {
        // Integration smoke test that database schema can be set up
        // This verifies the database handler can initialize without errors
        assertDoesNotThrow(() -> {
            var dbHandler = new dev.notequest.handler.DatabaseHandler();
            assertNotNull(dbHandler, "DatabaseHandler should be created successfully");
        }, "Database schema setup should not throw exception");
    }
}
