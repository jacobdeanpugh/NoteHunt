package dev.notequest.service;

import java.io.IOException;
import java.lang.Thread;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.time.Instant;

import com.google.common.eventbus.EventBus;
import dev.notequest.events.*;
import dev.notequest.handler.EventBusRegistry;
import dev.notequest.service.FileResult.*;
import dev.notequest.util.ConfigProvider;

/**
 * FileWatcherService is a background thread that monitors a directory for file system changes.
 * It tracks file creation, modification, and deletion events across the entire directory tree.
 * This service integrates with the file state database to maintain up-to-date file information.
 * 
 * The service uses Java's WatchService API with extended modifiers to monitor subdirectories
 * recursively and only processes files with specified extensions.
 * 
 * @author NoteQuest Development Team
 * @version 1.0
 * @since 1.0
 */
public class FileWatcherService extends Thread {
    
    // Core service components
    private WatchService watchService;
    private Path dirPath;
    private EventBus bus;
    
    /**
     * Standard file system events that the WatchService will monitor.
     * This configuration tracks all basic file operations: creation, deletion, and modification.
     * 
     * WARNING: Do not modify this array as it may break the file monitoring functionality.
     */
    public static final WatchEvent.Kind<?>[] STANDARD_WATCH_EVENT_KINDS = {
        StandardWatchEventKinds.ENTRY_CREATE,
        StandardWatchEventKinds.ENTRY_DELETE,
        StandardWatchEventKinds.ENTRY_MODIFY
    };
    
    /**
     * Supported file extensions that the service will process.
     * Currently configured to monitor only text files (.txt).
     * Additional extensions can be added to expand monitoring scope.
     */
    public static final String[] FILE_EXTENSIONS = {
        FileTreeCrawler.FileExtention.TXT
    };
    
    /**
     * Initializes the FileWatcherService with a target directory to monitor.
     * 
     * Sets up the WatchService with recursive directory monitoring using FILE_TREE modifier.
     * This allows the service to detect changes in subdirectories without manual registration.
     * 
     * @param directoryPath The absolute or relative path to the directory to monitor
     * @throws RuntimeException if the WatchService cannot be initialized, the path is invalid,
     *                         or any other unexpected error occurs during setup
     */
    public FileWatcherService() {
        super("file-watcher");
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
            this.dirPath = Paths.get(ConfigProvider.instance.getDirectoryPath());
            this.bus = bus;

            // Register the directory with recursive monitoring enabled

            Files.walk(dirPath)
                .filter(Files::isDirectory)
                .forEach(path -> {
                    try {
                        path.register(watchService, STANDARD_WATCH_EVENT_KINDS);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to register directory: " + path, e);
                    }
                });

        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize WatchService", e);
        } catch (InvalidPathException e) {
            throw new RuntimeException("Invalid path: " + this.dirPath, e);
        } catch (Exception e) {
            throw new RuntimeException("An unexpected exception occurred during initialization", e);
        }
    }

    /**
     * Constructor for testing - accepts a directory path and EventBus.
     * Allows tests to provide a temp directory and mock EventBus.
     *
     * @param dirPath Path to monitor
     * @param bus EventBus for posting events
     * @throws IOException if WatchService initialization fails
     */
    public FileWatcherService(Path dirPath, EventBus bus) throws IOException {
        super("file-watcher");
        this.watchService = FileSystems.getDefault().newWatchService();
        this.dirPath = dirPath;
        this.bus = bus;

        // Register the directory with recursive monitoring enabled
        Files.walk(dirPath)
            .filter(Files::isDirectory)
            .forEach(path -> {
                try {
                    path.register(watchService, STANDARD_WATCH_EVENT_KINDS);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to register directory: " + path, e);
                }
            });
    }

    /**
     * Registers a directory with the WatchService if not already registered.
     * Called when ENTRY_CREATE events are detected on directories.
     *
     * @param dirPath Path to directory to register
     */
    private void registerDirectory(Path dirPath) {
        try {
            if (Files.isDirectory(dirPath)) {
                dirPath.register(watchService, STANDARD_WATCH_EVENT_KINDS);
                System.out.println("Registered new directory: " + dirPath);
            }
        } catch (IOException e) {
            System.err.println("Failed to register directory: " + dirPath);
            e.printStackTrace();
        }
    }

    Boolean fileIsInExtensionFilter(String path) {
        // If no extensions are specified, include all files (no filtering)
        if (FILE_EXTENSIONS.length < 1) {
            return true;
        }

        // Check if the file ends with any of the target extensions
        for (String ext : FILE_EXTENSIONS) {
            if (path.toString().endsWith(ext)) {
                return true;
            }
        }

        // File doesn't match any target extension
        return false;
    }
    
    /**
     * Main monitoring loop that continuously watches for file system events.
     * 
     * This method runs indefinitely, blocking on watchService.take() until events occur.
     * When events are detected, it processes each one and outputs event details.
     * The method handles WatchKey reset to continue monitoring after processing events.
     * 
     * This is a private method called by the run() method when the thread starts.
     * 
     * @throws RuntimeException if the thread is interrupted or an unexpected error occurs
     */
    private void startWatchingDirectory() {
        try {
            WatchKey key;
            while ((key = this.watchService.take()) != null) {

                // Process all pending events for this key
                for (WatchEvent<?> event : key.pollEvents()) {
                    try {
                        Path full = dirPath.resolve(event.context().toString());

                        // Check if this event is a directory creation
                        if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                            Path eventPath = dirPath.resolve(event.context().toString());
                            if (Files.isDirectory(eventPath)) {
                                registerDirectory(eventPath);
                            }
                        }

                        FileResult fileResult = getFileResultFromFileChange(full, event.kind());

                        if (fileIsInExtensionFilter(full.toString())) {
                            try {
                                bus.post(new FileChangeEvent(fileResult));
                            } catch (Exception e) {
                                System.err.println("Failed to post file change event for: " + full);
                                e.printStackTrace();
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error processing watch event: " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                boolean valid = key.reset();
                if (!valid) {
                    System.err.println("WatchKey no longer valid, stopping watcher");
                    break;
                }
            }
        } catch (InterruptedException e) {
            System.err.println("FileWatcherService thread interrupted");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("Fatal error in FileWatcherService watch loop: " + e.getMessage());
            e.printStackTrace();
        }
    }

    FileResult getFileResultFromFileChange(Path filePath, WatchEvent.Kind<?> kind) {
        FileResult fileResult;

        try {

            // File was deleted
            if (kind.name().equals("ENTRY_DELETE")) {
                FileTime lastModified = FileTime.from(Instant.now());
                fileResult = new FileResult(filePath, FileStatus.DELETED, lastModified);

                return fileResult;
            }

            // File is created or modifed
            FileTime lastModified = Files.getLastModifiedTime(filePath);
            fileResult = new FileResult(filePath, FileStatus.PENDING, lastModified);

        } catch (Exception e) {
            fileResult = new FileResult(filePath, FileResult.FileStatus.ERROR, e);
        }

        return fileResult;
    }
    
    /**
     * Performs an initial scan of all files in the monitored directory tree.
     * 
     * Uses FileTreeCrawler to recursively traverse the directory structure and
     * identify all files matching the configured extensions. This method is typically
     * called during at the start of the thread to establish the current state of the directory.
     * 
     * The results are printed to the console for debugging purposes.
     * 
     * @throws RuntimeException if an error occurs during the file tree traversal
     */
    private void getAllDirectoryFiles() {
        try {
            FileTreeCrawler fileTreeCrawler = new FileTreeCrawler(FILE_EXTENSIONS);
            Files.walkFileTree(this.dirPath, fileTreeCrawler);
            bus.post(new FileTreeCrawledEvent(fileTreeCrawler.getResults()));
        } catch (Exception e) {
            throw new RuntimeException("An unexpected error occurred during file tree crawling", e);
        }
    }
    
    /**
     * Thread entry point that starts the file monitoring process.
     * 
     * This method is called when the thread is started via start() method.
     * It initiates the continuous directory monitoring by calling startWatchingDirectory().
     * 
     * The thread will run until interrupted or an unrecoverable error occurs.
     */
    @Override
    public void run() {
        // Start the primary file monitoring process
        getAllDirectoryFiles();
        startWatchingDirectory();
    }

    /**
     * Properly closes the WatchService and releases resources.
     * Should be called before shutting down the application.
     *
     * @throws IOException if closing the WatchService fails
     */
    public void close() throws IOException {
        try {
            if (watchService != null) {
                watchService.close();
                System.out.println("FileWatcherService closed successfully");
            }
        } catch (IOException e) {
            System.err.println("Error closing WatchService: " + e.getMessage());
            throw e;
        }
    }
}