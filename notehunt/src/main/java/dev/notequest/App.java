package dev.notequest;
import dev.notequest.service.FileWatcherService;
import dev.notequest.handler.*;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        DatabaseHandler dbHandler = new DatabaseHandler();
        EventBusRegistry.bus().register(dbHandler);

        new FileWatcherService().start();
    }
}
