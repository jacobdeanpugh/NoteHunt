package dev.notequest.handler.events;

import dev.notequest.service.FileTreeCrawler.*;
import java.util.ArrayList;

public class FileTreeCrawledEvent {
    private ArrayList<FileResult> fileResults;

    public FileTreeCrawledEvent(ArrayList<FileResult> fileResults) {
        this.fileResults = fileResults;
    }

    public ArrayList<FileResult> getFileResults() {
        return this.fileResults;
    }
}
