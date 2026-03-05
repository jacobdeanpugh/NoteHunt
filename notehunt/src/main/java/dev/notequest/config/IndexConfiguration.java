package dev.notequest.config;

import dev.notequest.service.FileIndexer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot configuration for Lucene indexing components.
 * Provides FileIndexer as a singleton bean for use throughout the application.
 */
@Configuration
public class IndexConfiguration {

    /**
     * Provide FileIndexer as Spring bean.
     * FileIndexer manages the Lucene index and provides IndexSearcher for queries.
     * Created as a singleton to ensure all components share the same index.
     *
     * @return FileIndexer singleton bean
     */
    @Bean
    public FileIndexer fileIndexer() {
        return new FileIndexer();
    }
}
