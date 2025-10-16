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
import dev.notequest.handler.EventBusRegistry;

public class FileIndexer {

    private StandardAnalyzer analyzer;
    private Directory indexDirectory;
    private IndexWriterConfig config;
    private IndexWriter writer;

    public FileIndexer() {
        try {
            analyzer = new StandardAnalyzer();
            indexDirectory = FSDirectory.open(Paths.get(ConfigProvider.instance.getIndexPath()));
            config = new IndexWriterConfig(analyzer);
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            writer = new IndexWriter(indexDirectory, config);
            System.out.println(indexDirectory.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void IndexFile(Path filePath) throws IOException {
        System.out.println("Indexing File : " + filePath.toAbsolutePath());
        Document doc = new Document();

        doc.add(new StringField("path", filePath.toString(), Field.Store.YES));

        String content = Files.readString(filePath);
        doc.add(new StringField("contents", content, Field.Store.YES));

        writer.addDocument(doc);
    }

    public ArrayList<FileResult> requestPendingFiles() {
        System.err.println("Requesting Files");
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

}
