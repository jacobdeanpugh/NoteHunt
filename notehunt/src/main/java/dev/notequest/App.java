package dev.notequest;
import dev.notequest.service.FileWatcherService;
import dev.notequest.doa.DatabaseHandler;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        DatabaseHandler dbHandler = new DatabaseHandler();
        dbHandler.testDatabaseConnection();
    }
}
