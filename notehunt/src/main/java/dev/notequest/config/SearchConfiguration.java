package dev.notequest.config;

import dev.notequest.search.SnippetExtractor;
import dev.notequest.search.SearchResultHandler;
import dev.notequest.search.RankingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot configuration for search-related beans.
 * Provides dependency injection wiring for SnippetExtractor and SearchResultHandler.
 */
@Configuration
public class SearchConfiguration {

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
     * Provide SearchResultHandler as Spring bean.
     * SearchResultHandler executes search queries against Lucene index and formats results.
     * Note: IndexSearcher will be injected later when FileIndexer is refactored to expose it as a bean.
     *
     * @param snippetExtractor autowired SnippetExtractor bean
     * @param rankingStrategy autowired RankingStrategy bean
     * @return SearchResultHandler singleton bean
     */
    @Bean
    public SearchResultHandler searchResultHandler(
            SnippetExtractor snippetExtractor,
            RankingStrategy rankingStrategy) {
        // Note: IndexSearcher will be injected when FileIndexer refactoring is complete
        // For now, this provides the basic wiring - null IndexSearcher will fail at runtime
        // until FileIndexer exposes IndexSearcher as a bean or via dependency injection
        return new SearchResultHandler(null, snippetExtractor, rankingStrategy);
    }
}
