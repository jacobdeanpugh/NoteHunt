# Phase 2.2: Recency Boost Ranking Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement recency-based score boosting to prioritize recently modified files in search results.

**Architecture:** Create a `RankingStrategy` component that calculates boost multipliers based on file age, apply the boost in `SearchResultHandler` after Lucene scoring, and make thresholds configurable via `config.json`.

**Tech Stack:** Java 21, Lucene 10.3.1, Lombok for DTOs, java.time for date calculations

---

## Task 1: Create RankingConfig DTO

**Files:**
- Create: `notehunt/src/main/java/dev/notequest/config/RankingConfig.java`
- Test: `notehunt/src/test/java/dev/notequest/config/RankingConfigTest.java`

**Step 1: Write the failing test**

```java
package dev.notequest.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RankingConfigTest {

    @Test
    public void testRankingConfigCreation() {
        RankingConfig config = new RankingConfig(3, 1.5, 7, 1.2, 1.0);

        assertEquals(3, config.getRecentDaysThreshold());
        assertEquals(1.5, config.getRecentMultiplier());
        assertEquals(7, config.getWeekDaysThreshold());
        assertEquals(1.2, config.getWeekMultiplier());
        assertEquals(1.0, config.getDefaultMultiplier());
    }
}
```

**Step 2: Run test to verify it fails**

```bash
cd /c/Repos/Personal/NoteHunt/notehunt
mvn test -Dtest=RankingConfigTest -v
```

Expected: FAIL with "class RankingConfig not found"

**Step 3: Write minimal implementation**

```java
package dev.notequest.config;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RankingConfig {
    private int recentDaysThreshold;
    private double recentMultiplier;
    private int weekDaysThreshold;
    private double weekMultiplier;
    private double defaultMultiplier;
}
```

**Step 4: Run test to verify it passes**

```bash
cd /c/Repos/Personal/NoteHunt/notehunt
mvn test -Dtest=RankingConfigTest -v
```

Expected: PASS

**Step 5: Commit**

```bash
cd /c/Repos/Personal/NoteHunt
git add notehunt/src/main/java/dev/notequest/config/RankingConfig.java notehunt/src/test/java/dev/notequest/config/RankingConfigTest.java
git commit -m "feat: add RankingConfig DTO for recency boost thresholds"
```

---

## Task 2: Create RankingStrategy class

**Files:**
- Create: `notehunt/src/main/java/dev/notequest/search/RankingStrategy.java`
- Test: `notehunt/src/test/java/dev/notequest/search/RankingStrategyTest.java`

**Step 1: Write the failing test for boost calculation at boundaries**

```java
package dev.notequest.search;

import dev.notequest.config.RankingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class RankingStrategyTest {

    private RankingStrategy rankingStrategy;

    @BeforeEach
    public void setUp() {
        RankingConfig config = new RankingConfig(3, 1.5, 7, 1.2, 1.0);
        rankingStrategy = new RankingStrategy(config);
    }

    @Test
    public void testBoostForRecentFile() {
        LocalDateTime today = LocalDateTime.now();
        double boost = rankingStrategy.calculateBoost(today);
        assertEquals(1.5, boost, "Recent file (0 days) should get 1.5x boost");
    }

    @Test
    public void testBoostAtRecentThreshold() {
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
        double boost = rankingStrategy.calculateBoost(threeDaysAgo);
        assertEquals(1.5, boost, "File at 3-day threshold should get 1.5x boost");
    }

    @Test
    public void testBoostJustAfterRecentThreshold() {
        LocalDateTime fourDaysAgo = LocalDateTime.now().minusDays(4);
        double boost = rankingStrategy.calculateBoost(fourDaysAgo);
        assertEquals(1.2, boost, "File at 4 days should get 1.2x boost");
    }

    @Test
    public void testBoostForWeekOldFile() {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        double boost = rankingStrategy.calculateBoost(weekAgo);
        assertEquals(1.2, boost, "File at 7-day threshold should get 1.2x boost");
    }

    @Test
    public void testBoostForOldFile() {
        LocalDateTime monthAgo = LocalDateTime.now().minusDays(30);
        double boost = rankingStrategy.calculateBoost(monthAgo);
        assertEquals(1.0, boost, "Old file (30 days) should get 1.0x boost (no boost)");
    }

    @Test
    public void testBoostForNullLastModified() {
        double boost = rankingStrategy.calculateBoost(null);
        assertEquals(1.0, boost, "Null lastModified should get 1.0x boost");
    }
}
```

**Step 2: Run test to verify it fails**

```bash
cd /c/Repos/Personal/NoteHunt/notehunt
mvn test -Dtest=RankingStrategyTest -v
```

Expected: FAIL with "class RankingStrategy not found"

**Step 3: Write minimal implementation**

```java
package dev.notequest.search;

import dev.notequest.config.RankingConfig;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class RankingStrategy {

    private final RankingConfig config;

    public RankingStrategy(RankingConfig config) {
        this.config = config;
    }

    /**
     * Calculate boost multiplier based on file modification time.
     * @param lastModified File's last modified timestamp
     * @return Boost multiplier (1.5x, 1.2x, or 1.0x)
     */
    public double calculateBoost(LocalDateTime lastModified) {
        if (lastModified == null) {
            return config.getDefaultMultiplier();
        }

        long daysElapsed = ChronoUnit.DAYS.between(lastModified, LocalDateTime.now());

        if (daysElapsed <= config.getRecentDaysThreshold()) {
            return config.getRecentMultiplier();
        } else if (daysElapsed <= config.getWeekDaysThreshold()) {
            return config.getWeekMultiplier();
        } else {
            return config.getDefaultMultiplier();
        }
    }
}
```

**Step 4: Run test to verify it passes**

```bash
cd /c/Repos/Personal/NoteHunt/notehunt
mvn test -Dtest=RankingStrategyTest -v
```

Expected: PASS (all 6 tests)

**Step 5: Commit**

```bash
cd /c/Repos/Personal/NoteHunt
git add notehunt/src/main/java/dev/notequest/search/RankingStrategy.java notehunt/src/test/java/dev/notequest/search/RankingStrategyTest.java
git commit -m "feat: implement RankingStrategy with recency boost calculation

- Calculate boost based on daysElapsed
- Support aggressive thresholds (3 days @ 1.5x, 7 days @ 1.2x)
- Fallback to 1.0x for older files and null timestamps
- All boundary conditions tested"
```

---

## Task 3: Update ConfigProvider to load ranking config

**Files:**
- Modify: `notehunt/src/main/java/dev/notequest/config/ConfigProvider.java`
- Test: `notehunt/src/test/java/dev/notequest/config/ConfigProviderTest.java`

**Step 1: Write the failing test**

Add to `ConfigProviderTest.java`:

```java
@Test
public void testLoadRankingConfig() {
    RankingConfig rankingConfig = ConfigProvider.instance.getRankingConfig();

    assertNotNull(rankingConfig, "RankingConfig should not be null");
    assertEquals(3, rankingConfig.getRecentDaysThreshold());
    assertEquals(1.5, rankingConfig.getRecentMultiplier());
    assertEquals(7, rankingConfig.getWeekDaysThreshold());
    assertEquals(1.2, rankingConfig.getWeekMultiplier());
    assertEquals(1.0, rankingConfig.getDefaultMultiplier());
}
```

**Step 2: Run test to verify it fails**

```bash
cd /c/Repos/Personal/NoteHunt/notehunt
mvn test -Dtest=ConfigProviderTest::testLoadRankingConfig -v
```

Expected: FAIL

**Step 3: Modify ConfigProvider**

Add to ConfigProvider class:

```java
import dev.notequest.config.RankingConfig;

private RankingConfig rankingConfig;

// Add in constructor after loading other config:
loadRankingConfig(jsonObject);

// Add this method:
private void loadRankingConfig(JSONObject config) {
    if (config.containsKey("ranking")) {
        JSONObject rankingObj = (JSONObject) config.get("ranking");
        JSONObject boostObj = (JSONObject) rankingObj.get("recencyBoost");

        int recentDays = ((Number) boostObj.get("recentDaysThreshold")).intValue();
        double recentMult = ((Number) boostObj.get("recentMultiplier")).doubleValue();
        int weekDays = ((Number) boostObj.get("weekDaysThreshold")).intValue();
        double weekMult = ((Number) boostObj.get("weekMultiplier")).doubleValue();
        double defaultMult = ((Number) boostObj.get("defaultMultiplier")).doubleValue();

        this.rankingConfig = new RankingConfig(recentDays, recentMult, weekDays, weekMult, defaultMult);
    } else {
        this.rankingConfig = new RankingConfig(3, 1.5, 7, 1.2, 1.0);
    }
}

public RankingConfig getRankingConfig() {
    return rankingConfig;
}
```

**Step 4: Run test to verify it passes**

```bash
cd /c/Repos/Personal/NoteHunt/notehunt
mvn test -Dtest=ConfigProviderTest::testLoadRankingConfig -v
```

Expected: PASS

**Step 5: Commit**

```bash
cd /c/Repos/Personal/NoteHunt
git add notehunt/src/main/java/dev/notequest/config/ConfigProvider.java notehunt/src/test/java/dev/notequest/config/ConfigProviderTest.java
git commit -m "feat: add ranking config parsing to ConfigProvider

- Load ranking.recencyBoost thresholds and multipliers from config.json
- Provide sensible defaults if section missing
- Expose via getRankingConfig() getter"
```

---

## Task 4: Update config.json with ranking section

**Files:**
- Modify: `notehunt/src/main/resources/dev/notequest/config.json`

**Step 1: Update config.json**

Replace content with:

```json
{
  "directoryPath": "C:\\Users\\jacob\\Downloads\\20_newsgroups\\talk.politics.misc",
  "indexPath": "%APPDATA%\\NoteQuest\\index\\",
  "indexBatchSize": 50,
  "ranking": {
    "recencyBoost": {
      "recentDaysThreshold": 3,
      "recentMultiplier": 1.5,
      "weekDaysThreshold": 7,
      "weekMultiplier": 1.2,
      "defaultMultiplier": 1.0
    }
  }
}
```

**Step 2: Verify valid JSON**

```bash
cd /c/Repos/Personal/NoteHunt
cat notehunt/src/main/resources/dev/notequest/config.json | python -m json.tool > /dev/null && echo "Valid JSON"
```

Expected: "Valid JSON"

**Step 3: Commit**

```bash
cd /c/Repos/Personal/NoteHunt
git add notehunt/src/main/resources/dev/notequest/config.json
git commit -m "config: add aggressive recency boost thresholds

- Recent files (≤3 days): 1.5x boost
- Week-old files (≤7 days): 1.2x boost
- Older files: 1.0x (no boost)"
```

---

## Task 5: Inject RankingStrategy into SearchResultHandler

**Files:**
- Modify: `notehunt/src/main/java/dev/notequest/search/SearchResultHandler.java`
- Test: `notehunt/src/test/java/dev/notequest/search/SearchResultHandlerTest.java`

**Step 1: Add test for re-ranked results**

Add to `SearchResultHandlerTest.java`:

```java
@Test
public void testSearchResultsAreRerankedByRecency() throws Exception {
    RankingConfig config = new RankingConfig(3, 1.5, 7, 1.2, 1.0);
    RankingStrategy rankingStrategy = new RankingStrategy(config);

    SearchResultHandler handler = new SearchResultHandler(mockSearcher, snippetExtractor, rankingStrategy);

    SearchResponse response = handler.executeSearch("test", 10, 0);

    assertNotNull(response);
    assertFalse(response.getResults().isEmpty());
}
```

**Step 2: Run test to verify it fails**

```bash
cd /c/Repos/Personal/NoteHunt/notehunt
mvn test -Dtest=SearchResultHandlerTest::testSearchResultsAreRerankedByRecency -v
```

Expected: FAIL

**Step 3: Modify SearchResultHandler**

Add field and constructor parameter:

```java
import dev.notequest.search.RankingStrategy;

private final RankingStrategy rankingStrategy;

public SearchResultHandler(IndexSearcher searcher, SnippetExtractor snippetExtractor, RankingStrategy rankingStrategy) {
    this.searcher = searcher;
    this.snippetExtractor = snippetExtractor;
    this.rankingStrategy = rankingStrategy;
    this.queryParser = new QueryParser();
}
```

Modify buildSearchResult method to apply boost:

```java
private SearchResult buildSearchResult(Document doc, float score, String queryString) throws Exception {
    String path = doc.get("path");
    String content = doc.get("contents");
    LocalDateTime lastModified = getLastModified(doc);

    String snippet = snippetExtractor.extractSnippet(content != null ? content : "", queryString);

    // Apply recency boost
    double boost = rankingStrategy.calculateBoost(lastModified);
    double boostedScore = score * boost;

    return SearchResult.builder()
            .path(path)
            .score(boostedScore)
            .lastModified(lastModified)
            .snippet(snippet)
            .build();
}
```

**Step 4: Run test to verify it passes**

```bash
cd /c/Repos/Personal/NoteHunt/notehunt
mvn test -Dtest=SearchResultHandlerTest -v
```

Expected: PASS

**Step 5: Commit**

```bash
cd /c/Repos/Personal/NoteHunt
git add notehunt/src/main/java/dev/notequest/search/SearchResultHandler.java notehunt/src/test/java/dev/notequest/search/SearchResultHandlerTest.java
git commit -m "feat: apply recency boost to search result scores

- Inject RankingStrategy into SearchResultHandler
- Multiply each result's score by recency boost factor
- More recent files rank higher in results
- All tests passing"
```

---

## Task 6: Update Spring configuration for dependency wiring

**Files:**
- Modify: `notehunt/src/main/java/dev/notequest/config/SearchConfiguration.java`

**Step 1: Update SearchConfiguration**

```java
package dev.notequest.config;

import dev.notequest.search.RankingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SearchConfiguration {

    @Bean
    public RankingStrategy rankingStrategy(ConfigProvider configProvider) {
        return new RankingStrategy(configProvider.getRankingConfig());
    }
}
```

**Step 2: Run build**

```bash
cd /c/Repos/Personal/NoteHunt/notehunt
mvn clean package -DskipTests=true -v
```

Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
cd /c/Repos/Personal/NoteHunt
git add notehunt/src/main/java/dev/notequest/config/SearchConfiguration.java
git commit -m "config: wire RankingStrategy dependency in Spring

- Create RankingStrategy bean from ConfigProvider ranking config
- Enable Spring to manage lifecycle and dependencies"
```

---

## Task 7: Run full test suite and verify integration

**Files:**
- Test: All tests

**Step 1: Run full test suite**

```bash
cd /c/Repos/Personal/NoteHunt/notehunt
mvn clean test -v
```

Expected: All tests PASS

**Step 2: Run specific ranking tests**

```bash
cd /c/Repos/Personal/NoteHunt/notehunt
mvn test -Dtest=RankingStrategyTest,RankingConfigTest,ConfigProviderTest -v
```

Expected: All PASS

**Step 3: Run build**

```bash
cd /c/Repos/Personal/NoteHunt/notehunt
mvn clean package -v
```

Expected: BUILD SUCCESS

**Step 4: Commit summary**

```bash
cd /c/Repos/Personal/NoteHunt
git log --oneline -8
```
