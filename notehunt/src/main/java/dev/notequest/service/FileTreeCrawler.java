package dev.notequest.service;

import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.Path;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;

import dev.notequest.service.FileTreeCrawler.FileResult.FileStatus;

public class FileTreeCrawler extends SimpleFileVisitor<Path> {
    // FileResult is a custom class dealing with results from the FileTreeCrawlwer
    public static class FileResult {
        public enum FileStatus {SUCCESS, ERROR}
        private Path filePath;
        private FileStatus fileStatus;
        private FileTime lastModified;

        public FileResult(Path filePath, FileStatus fileStatus, FileTime lastModified) {
            this.filePath = filePath;
            this.fileStatus = fileStatus;
            this.lastModified = lastModified;
        }

        public Path getPath() {return this.filePath;}
        public FileStatus getFileStatus() {return this.fileStatus;}
        public FileTime getLastModified() {return this.lastModified;}

        @Override
        public String toString() {return this.filePath + " " + this.fileStatus + " " + this.lastModified;}
    }

    private ArrayList<FileResult> fileTreeCrawerResults = new ArrayList<FileResult>(); 
    
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
        if (attr.isRegularFile()) {
            FileResult result = new FileResult(file, FileStatus.SUCCESS, attr.lastModifiedTime());
            fileTreeCrawerResults.add(result);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        FileResult result = new FileResult(file, FileStatus.ERROR, null);
        fileTreeCrawerResults.add(result);

        return FileVisitResult.CONTINUE;
    }

    public ArrayList<FileResult> getResults() {
        return fileTreeCrawerResults;
    }
}
