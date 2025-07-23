package dev.notequest.events;

import dev.notequest.service.FileResult;
import java.util.ArrayList;

public class FileTreeCrawledEvent {
    private ArrayList<FileResult> fileResults;

    public FileTreeCrawledEvent(ArrayList<FileResult> fileResults) {
        this.fileResults = fileResults;
    }

    public FileResult[] getFileResults() {
        return this.fileResults.toArray(new FileResult[0]);
    }
}
