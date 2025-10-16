package dev.notequest.events;

import dev.notequest.service.FileResult;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class PendingFilesRequestEvent {
    
    private final CompletableFuture<ArrayList<FileResult>> replyFuture;

    public PendingFilesRequestEvent(CompletableFuture<ArrayList<FileResult>> replyFuture) {
        this.replyFuture = replyFuture;
    }

    public CompletableFuture<ArrayList<FileResult>> getFuture() {
        return this.replyFuture;
    }
}
