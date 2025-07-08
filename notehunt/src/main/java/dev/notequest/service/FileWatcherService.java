package dev.notequest.service;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchService;
import java.nio.file.WatchEvent;

class FilesWatcherService {
    WatchService watchService;
    Path dirPath;

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
    public FilesWatcherService(String directoryPath) {
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
            this.dirPath = Paths.get(directoryPath);

            dirPath.register(watchService, STANDARD_WATCH_EVENT_KINDS);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize WatchService", e);
        }
    }

}