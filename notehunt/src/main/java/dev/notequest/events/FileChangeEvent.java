package dev.notequest.events;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import dev.notequest.util.MD5Util;

public class FileChangeEvent {
    private final String filePath;
    private final String filePathHash;
    private final WatchEvent.Kind<?> kind;
    
    public FileChangeEvent(Path path, WatchEvent.Kind<?> kind) {
        this.filePath = path.toString();
        this.filePathHash = MD5Util.md5Hex(path.toString());
        this.kind = kind;
    }

    public String getPath() {return filePath;}
    public String getFilePathHash() {return filePathHash;}
    public WatchEvent.Kind<?> getKind() {return kind;}
}
