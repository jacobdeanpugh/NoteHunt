package dev.notequest;
import dev.notequest.service.FileWatcherService;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import dev.notequest.handler.*;
import dev.notequest.service.FileIndexer;;

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

        // new FileWatcherService().start();
        System.err.println(new FileIndexer().requestPendingFiles());
    }
}
