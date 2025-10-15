package dev.notequest.util;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ConfigProvider {
    
    private static final String configResourcePath = "/dev/notequest/config.json";
    private String directoryPath;
    private String indexPath;

    public static final ConfigProvider instance = new ConfigProvider();

    private ConfigProvider() {
        loadConfig();
    }

    private void loadConfig() {
        JSONParser parser = new JSONParser();
        
        // Use try-with-resources to automatically close the streams.
        // This method loads the file from the classpath, which works inside a JAR.
        try (InputStream inputStream = ConfigProvider.class.getResourceAsStream(configResourcePath)) {
            
            // Check if the resource was found
            if (inputStream == null) {
                throw new RuntimeException("Cannot find resource file: " + configResourcePath + ". Make sure it's in src/main/resources.");
            }
            
            // The JSON parser needs a character-based Reader, so we wrap the byte-based InputStream.
            // Specifying UTF-8 is a good practice for cross-platform compatibility.
            try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                JSONObject jsonObject = (JSONObject) parser.parse(reader);
                this.directoryPath = (String) jsonObject.get("directoryPath");
                this.indexPath = (String) jsonObject.get("indexPath");
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load configuration from resource: " + configResourcePath, e);
        }
    }

    public String getDirectoryPath() {
        return directoryPath;
    }

    public String getIndexPath() {
        return indexPath.replace("%APPDATA%", System.getenv("APPDATA"));
    }
}