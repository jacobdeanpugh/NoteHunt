package dev.notequest;
import dev.notequest.service.FileWatcherService;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        String path = "C:\\Users\\jacob\\OneDrive\\Documents\\notestesting";
        FileWatcherService fileWatcherService = new FileWatcherService(path);
        fileWatcherService.startWatchingDirectory();
    }
}
