package dev.notequest.service;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.*;
import org.apache.lucene.search.IndexSearcher;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import com.google.common.eventbus.EventBus;
import dev.notequest.util.ConfigProvider;
import dev.notequest.events.PendingFilesRequestEvent;
import dev.notequest.events.SetFilesToCompleteEvent;
import dev.notequest.handler.EventBusRegistry;

public class FileIndexer implements AutoCloseable {

    private StandardAnalyzer analyzer;
    private Directory indexDirectory;
    private IndexWriterConfig config;
    private IndexWriter writer;
    private int indexBatchSize;
    private EventBus bus;
    private DirectoryReader cachedReader;
    private IndexSearcher cachedSearcher;

    public FileIndexer() {
        try {
            analyzer = new StandardAnalyzer();
            indexDirectory = FSDirectory.open(Paths.get(ConfigProvider.instance.getIndexPath()));
            config = new IndexWriterConfig(analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            writer = new IndexWriter(indexDirectory, config);
            indexBatchSize = ConfigProvider.instance.getIndexBatchSize();
            bus = EventBusRegistry.bus();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Constructor for testing - accepts a Lucene Directory, batch size, and EventBus.
     * Allows tests to provide an in-memory ByteBuffersDirectory and mock EventBus.
     *
     * @param directory Lucene Directory (e.g., ByteBuffersDirectory for RAM)
     * @param batchSize Number of files to index per batch
     * @param bus EventBus for posting events (can be mock)
     * @throws IOException if IndexWriter initialization fails
     */
    public FileIndexer(Directory directory, int batchSize, EventBus bus) throws IOException {
        analyzer = new StandardAnalyzer();
        indexDirectory = directory;
        config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        writer = new IndexWriter(indexDirectory, config);
        indexBatchSize = batchSize;
        this.bus = bus;
    }

    public void indexFile(Path filePath) throws IOException {
        Document doc = new Document();
        doc.add(new StringField("path", filePath.toString(), Field.Store.YES));

        String content = Files.readString(filePath);
        doc.add(new TextField("contents", content, Field.Store.YES));  // Store.YES for snippets

        doc.add(new StoredField("fileSize", Long.toString(Files.size(filePath))));
        doc.add(new StoredField("lastModified", Long.toString(
            Files.getLastModifiedTime(filePath).toMillis()
        )));

        writer.addDocument(doc);
    }

    public ArrayList<FileResult> requestPendingFiles() {
        CompletableFuture<ArrayList<FileResult>> replyFuture = new CompletableFuture<ArrayList<FileResult>> ();
        PendingFilesRequestEvent requestEvent = new PendingFilesRequestEvent(replyFuture);

        bus.post(requestEvent);

        try {
             ArrayList<FileResult> results = replyFuture.get(10, java.util.concurrent.TimeUnit.SECONDS);
             return results;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<FileResult>();
        }
    }    

    /**
     * Get or create an IndexSearcher for the current Lucene index.
     * The IndexSearcher is cached after first creation.
     * Callers should NOT close the returned searcher; it is managed by FileIndexer.
     *
     * @return IndexSearcher for querying the index, or null if index cannot be opened
     * @throws IOException if the index directory cannot be read
     */
    public IndexSearcher getSearcher() throws IOException {
        // Commit any pending changes before opening reader
        writer.commit();

        // If we have a cached searcher, check if the underlying reader is still current
        if (cachedSearcher != null && cachedReader != null) {
            try {
                DirectoryReader newReader = DirectoryReader.openIfChanged(cachedReader);
                if (newReader == null) {
                    // No changes, return existing searcher
                    return cachedSearcher;
                } else {
                    // Index has changed, close old reader and create new searcher
                    cachedReader.close();
                    cachedReader = newReader;
                    cachedSearcher = new IndexSearcher(cachedReader);
                    return cachedSearcher;
                }
            } catch (IOException e) {
                // If unable to check for changes, close cached and recreate
                if (cachedReader != null) {
                    try {
                        cachedReader.close();
                    } catch (IOException ignored) {
                    }
                }
                cachedReader = DirectoryReader.open(indexDirectory);
                cachedSearcher = new IndexSearcher(cachedReader);
                return cachedSearcher;
            }
        }

        // First time: create reader and searcher
        cachedReader = DirectoryReader.open(indexDirectory);
        cachedSearcher = new IndexSearcher(cachedReader);
        return cachedSearcher;
    }

    @Override
    public void close() throws IOException {
        // Close cached reader if it exists (which also affects cached searcher)
        if (cachedReader != null) {
            cachedReader.close();
            cachedReader = null;
            cachedSearcher = null;
        }

        writer.close();
        indexDirectory.close();
    }

    public void indexFilesFromDatabase() throws IOException {
        ArrayList<FileResult> pendingFiles = requestPendingFiles();

        for (int i = 0; i < pendingFiles.size(); i += indexBatchSize) {
            SetFilesToCompleteEvent event = new SetFilesToCompleteEvent();
            int end_index = i + indexBatchSize;

            end_index = end_index >= pendingFiles.size() ? pendingFiles.size() : i + indexBatchSize;
            for(FileResult fr : pendingFiles.subList(i, end_index)){
                try {
                    indexFile(fr.getPath());
                    event.addFileResult(fr);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            writer.commit();
            bus.post(event);
        }
    }
}
