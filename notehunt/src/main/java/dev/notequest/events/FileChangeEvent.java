package dev.notequest.events;
import java.nio.file.WatchEvent;
import dev.notequest.service.FileResult;

public class FileChangeEvent {
    private final FileResult fileResult;
    private final WatchEvent.Kind<?> kind;
    
    public FileChangeEvent(FileResult fileResult, WatchEvent.Kind<?> kind) {
        this.fileResult = fileResult;
        this.kind = kind;
    }

    public FileResult getFileResult() {return fileResult;}
    public WatchEvent.Kind<?> getKind() {return kind;}
}
