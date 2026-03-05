package dev.notequest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * NoteHunt - Full-text search service with Spring Boot
 */
@SpringBootApplication
public class App {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(App.class, args);
        // App is now running as Spring Boot server
        // Spring will auto-wire beans and start HTTP server on port 8080
    }
}
