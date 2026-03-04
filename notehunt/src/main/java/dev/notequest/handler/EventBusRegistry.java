package dev.notequest.handler;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import java.util.concurrent.Executors;

public class EventBusRegistry {
    private static final AsyncEventBus ORIGINAL_BUS = new AsyncEventBus("file-event-bus", Executors.newSingleThreadExecutor(
        r -> new Thread(r, "event-handler")
    ));
    private static EventBus currentBus = ORIGINAL_BUS;

    private EventBusRegistry() {};

    public static EventBus bus() {
        return currentBus;
    }

    // Package-private setter for tests
    static void setInstanceForTesting(EventBus testBus) {
        currentBus = testBus;
    }

    // Package-private reset for tests
    static void resetForTesting() {
        currentBus = ORIGINAL_BUS;
    }
}
