package dev.notequest.service;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.*;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import dev.notequest.util.ConfigProvider;
import dev.notequest.events.PendingFilesRequestEvent;
import dev.notequest.events.SetFilesToCompleteEvent;
import dev.notequest.handler.EventBusRegistry;

public class FileIndexer {

    private StandardAnalyzer analyzer;
    private Directory indexDirectory;
    private IndexWriterConfig config;
    private IndexWriter writer;
    private int indexBatchSize;

    public FileIndexer() {
        try {
            analyzer = new StandardAnalyzer();
            indexDirectory = FSDirectory.open(Paths.get(ConfigProvider.instance.getIndexPath()));
            config = new IndexWriterConfig(analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            writer = new IndexWriter(indexDirectory, config);
            indexBatchSize = ConfigProvider.instance.getIndexBatchSize();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void indexFile(Path filePath) throws IOException {
        Document doc = new Document();

        doc.add(new StringField("path", filePath.toString(), Field.Store.YES));

        String content = Files.readString(filePath);
        doc.add(new TextField("contents", content, Field.Store.NO));

        writer.addDocument(doc);
    }

    public ArrayList<FileResult> requestPendingFiles() {
        CompletableFuture<ArrayList<FileResult>> replyFuture = new CompletableFuture<ArrayList<FileResult>> ();
        PendingFilesRequestEvent requestEvent = new PendingFilesRequestEvent(replyFuture);

        EventBusRegistry.bus().post(requestEvent);

        try {
             ArrayList<FileResult> results = replyFuture.get();
             return results;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<FileResult>();
        }
    }    

    public void indexFilesFromDatabase() {
        ArrayList<FileResult> pendingFiles = requestPendingFiles();

        for (int i = 0; i < pendingFiles.size(); i += indexBatchSize) {
            SetFilesToCompleteEvent event = new SetFilesToCompleteEvent();
            int end_index = i + indexBatchSize;

            end_index = end_index >= pendingFiles.size() ? pendingFiles.size() - 1 : i + indexBatchSize;
            for(FileResult fr : pendingFiles.subList(i, end_index)){
                try {
                    indexFile(fr.getPath());
                    event.addFileResult(fr);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Index Count: " + i);
            EventBusRegistry.bus().post(event);
        }
    }
}
