package dev.notequest.handler;

import com.google.common.eventbus.AsyncEventBus;
import java.util.concurrent.Executors;

public class EventBusRegistry {
    private static final AsyncEventBus BUS = new AsyncEventBus("file-event-bus", Executors.newSingleThreadExecutor(
        r -> new Thread(r, "event-handler")
    ));

    private EventBusRegistry() {};

    public static AsyncEventBus bus() {
        return BUS;
    }
}
