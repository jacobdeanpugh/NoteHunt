package dev.notequest.events;
import dev.notequest.service.FileResult;

public class FileChangeEvent {
    private final FileResult fileResult;
    
    public FileChangeEvent(FileResult fileResult) {
        this.fileResult = fileResult;
    }

    public FileResult getFileResult() {return fileResult;}
}
