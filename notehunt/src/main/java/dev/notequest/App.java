package dev.notequest;
import dev.notequest.service.FileWatcherService;
import dev.notequest.doa.DatabaseHandler;
import dev.notequest.handler.*;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        EventBusRegistry.bus().register(new EventHandler());

        new FileWatcherService("C:\\Users\\jacob\\OneDrive\\Documents\\notestesting").run();
    }
}
