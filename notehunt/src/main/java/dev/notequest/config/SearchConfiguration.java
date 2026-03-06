package dev.notequest.config;

import dev.notequest.search.SnippetExtractor;
import dev.notequest.search.SearchResultHandler;
import dev.notequest.search.RankingStrategy;
import dev.notequest.service.FileIndexer;
import dev.notequest.util.ConfigProvider;
import org.apache.lucene.search.IndexSearcher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot configuration for search-related beans.
 * Provides dependency injection wiring for SnippetExtractor, SearchResultHandler, and IndexSearcher.
 */
@Configuration
public class SearchConfiguration {

    /**
     * Provide RankingConfig as Spring bean.
     * RankingConfig is loaded from ConfigProvider singleton and contains ranking algorithm parameters.
     *
     * @return RankingConfig singleton bean from ConfigProvider
     */
    @Bean
    public RankingConfig rankingConfig() {
        return ConfigProvider.instance.getRankingConfig();
    }

    /**
     * Provide SnippetExtractor as Spring bean.
     * SnippetExtractor is responsible for extracting query-highlighted snippets from documents.
     *
     * @return SnippetExtractor singleton bean
     */
    @Bean
    public SnippetExtractor snippetExtractor() {
        return new SnippetExtractor();
    }

    /**
     * Provide RankingStrategy as Spring bean.
     * RankingStrategy applies recency and other ranking factors to search results.
     *
     * @param rankingConfig autowired RankingConfig bean
     * @return RankingStrategy singleton bean
     */
    @Bean
    public RankingStrategy rankingStrategy(RankingConfig rankingConfig) {
        return new RankingStrategy(rankingConfig);
    }

    /**
     * Provide IndexSearcher as Spring bean.
     * IndexSearcher is obtained from FileIndexer lazily to ensure it's created
     * AFTER indexing has completed (not during startup).
     * The searcher is cached by FileIndexer and refreshed when the index changes.
     *
     * @param fileIndexer autowired FileIndexer bean
     * @return IndexSearcher for querying the Lucene index
     * @throws Exception if the IndexSearcher cannot be created
     */
    @Bean
    public IndexSearcher indexSearcher(FileIndexer fileIndexer) throws Exception {
        // Note: This bean is injected lazily into SearchResultHandler via SearchController.
        // By the time it's accessed (first search request), IndexingStartupComponent has
        // already completed indexing, so getSearcher() will return a populated index.
        return fileIndexer.getSearcher();
    }

    /**
     * Provide SearchResultHandler as Spring bean.
     * SearchResultHandler executes search queries against Lucene index and formats results.
     * It requires an IndexSearcher (obtained from FileIndexer) to query the index.
     *
     * @param indexSearcher autowired IndexSearcher bean (obtained from FileIndexer)
     * @param snippetExtractor autowired SnippetExtractor bean
     * @param rankingStrategy autowired RankingStrategy bean
     * @return SearchResultHandler singleton bean
     */
    @Bean
    public SearchResultHandler searchResultHandler(
            IndexSearcher indexSearcher,
            SnippetExtractor snippetExtractor,
            RankingStrategy rankingStrategy) {
        return new SearchResultHandler(indexSearcher, snippetExtractor, rankingStrategy);
    }
}
