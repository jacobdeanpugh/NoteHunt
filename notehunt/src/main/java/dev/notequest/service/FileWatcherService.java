package dev.notequest.service;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchService;

class FilesWatcherService {
    WatchService watchService;
    Path dirPath;

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
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize WatchService", e);
        }
    }

    
}