package dev.notequest.service;

import java.nio.file.SimpleFileVisitor;
import java.nio.file.Path;
import java.nio.file.FileVisitResult;

public class FileTreeCrawler extends SimpleFileVisitor<Path> {
    // FileResult is a custom class dealing with results from the FileTreeCrawlwer
    public static class FileResult {
        public enum FileStatus {SUCCESS, ERROR}
        private Path filePath;
        private FileStatus fileStatus;

        public FileResult(Path filePath, FileStatus fileStatus) {
            this.filePath = filePath;
            this.fileStatus = fileStatus;
        }

        public Path getPath() {return this.filePath;}
        public FileStatus  getFileStatus() {return this.fileStatus;}

        @Override
        public String toString() {return this.filePath + " " + this.fileStatus;}
    }
    
    
}
