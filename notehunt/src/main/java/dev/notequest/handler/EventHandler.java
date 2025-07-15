package dev.notequest.handler;

import com.google.common.eventbus.Subscribe;
import dev.notequest.handler.events.FileChangeEvent;

public class EventHandler {
    
    @Subscribe
    public void onFileChange(FileChangeEvent event) {
        System.out.println("Handling event on thread "
            + Thread.currentThread().getName()
            + ": " + event.getKind() + " -> " + event.getPath());
    }
}
