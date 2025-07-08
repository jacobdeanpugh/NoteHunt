package dev.notequest.service;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.TimeUnit;

public class FileWatcherService {
    // Class Variables
    WatchService watchService;
    Path dirPath;

    // This final handles what events the WatcherKeys will listen for. DO NOT CHANGE
    public static final WatchEvent.Kind<?>[] STANDARD_WATCH_EVENT_KINDS = {
        StandardWatchEventKinds.ENTRY_CREATE,
        StandardWatchEventKinds.ENTRY_DELETE,
        StandardWatchEventKinds.ENTRY_MODIFY
    };

    /*
    * FileWatcherService handles all functionality regarding analyzing and tracking file changes within a directory path.
    * This will create and handle any writes to the file state database. 
    *
    * @param directoryPath The directory path actively monitored by FileWatcherService
    * @throws IllegalArgumentException Either no directory path was null or invalid
    */
    public FileWatcherService(String directoryPath) {
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
            this.dirPath = Paths.get(directoryPath);

            dirPath.register(watchService, STANDARD_WATCH_EVENT_KINDS);

            
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize WatchService", e);
        }
    }
    
    public void startWatchingDirectory () {
        try {
            WatchKey key;

            while ((key = this.watchService.take()) != null) {
                for (WatchEvent<?> event: key.pollEvents()) {
                    System.out.println("Event Kind: " + event.kind() + ". File Affected: " + event.context() + ".");
                }
                key.reset();
            }

        } catch (InterruptedException e) {
            throw new RuntimeException("Thread was interrupted during sleep.");
        }
    }
}