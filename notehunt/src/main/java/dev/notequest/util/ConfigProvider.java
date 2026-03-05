package dev.notequest.util;

import dev.notequest.config.RankingConfig;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ConfigProvider {
    
    private static final String configResourcePath = "/dev/notequest/config.json";
    private String directoryPath;
    private String indexPath;
    private int indexBatchSize;
    private RankingConfig rankingConfig;

    public static final ConfigProvider instance = new ConfigProvider();

    private ConfigProvider() {
        loadConfig();
    }

    // Package-private constructor for testing
    ConfigProvider(String directoryPath, String indexPath, int indexBatchSize) {
        this.directoryPath = directoryPath;
        this.indexPath = indexPath;
        this.indexBatchSize = indexBatchSize;
        this.rankingConfig = new RankingConfig(3, 1.5, 7, 1.2, 1.0); // Default values
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
                this.indexBatchSize = ((Long) jsonObject.get("indexBatchSize")).intValue();
                loadRankingConfig(jsonObject);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load configuration from resource: " + configResourcePath, e);
        }
    }

    private void loadRankingConfig(JSONObject config) {
        try {
            if (config == null || !config.containsKey("ranking")) {
                this.rankingConfig = new RankingConfig(3, 1.5, 7, 1.2, 1.0);
                return;
            }

            Object rankingObj = config.get("ranking");
            if (!(rankingObj instanceof JSONObject)) {
                this.rankingConfig = new RankingConfig(3, 1.5, 7, 1.2, 1.0);
                return;
            }

            Object boostObj = ((JSONObject) rankingObj).get("recencyBoost");
            if (!(boostObj instanceof JSONObject)) {
                this.rankingConfig = new RankingConfig(3, 1.5, 7, 1.2, 1.0);
                return;
            }

            JSONObject boost = (JSONObject) boostObj;

            // Extract values with null checks and defaults
            Object recentDaysObj = boost.get("recentDaysThreshold");
            int recentDays = (recentDaysObj instanceof Number)
                ? ((Number) recentDaysObj).intValue()
                : 3;

            Object recentMultObj = boost.get("recentMultiplier");
            double recentMult = (recentMultObj instanceof Number)
                ? ((Number) recentMultObj).doubleValue()
                : 1.5;

            Object weekDaysObj = boost.get("weekDaysThreshold");
            int weekDays = (weekDaysObj instanceof Number)
                ? ((Number) weekDaysObj).intValue()
                : 7;

            Object weekMultObj = boost.get("weekMultiplier");
            double weekMult = (weekMultObj instanceof Number)
                ? ((Number) weekMultObj).doubleValue()
                : 1.2;

            Object defaultMultObj = boost.get("defaultMultiplier");
            double defaultMult = (defaultMultObj instanceof Number)
                ? ((Number) defaultMultObj).doubleValue()
                : 1.0;

            this.rankingConfig = new RankingConfig(recentDays, recentMult, weekDays, weekMult, defaultMult);
        } catch (Exception e) {
            // Fall back to defaults on any error
            System.err.println("Warning: Failed to parse ranking config, using defaults: " + e.getMessage());
            this.rankingConfig = new RankingConfig(3, 1.5, 7, 1.2, 1.0);
        }
    }

    public String getDirectoryPath() {
        return directoryPath;
    }

    public String getIndexPath() {
        return indexPath.replace("%APPDATA%", System.getenv("APPDATA"));
    }

    public int getIndexBatchSize() {
        return this.indexBatchSize;
    }

    public RankingConfig getRankingConfig() {
        return rankingConfig;
    }

    // Factory method for tests
    public static ConfigProvider forTesting(String dir, String indexPath, int batchSize) {
        return new ConfigProvider(dir, indexPath, batchSize);
    }
}