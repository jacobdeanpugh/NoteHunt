# Phase 2.2: Result Ranking & Recency Boost — Design Document

**Date:** 2026-03-04
**Phase:** 2.2 Result Enhancement
**Status:** Design Approved

---

## Overview

Implement recency-based score boosting to prioritize recently modified files in search results. This completes Phase 2 (Phase 2.1 snippet extraction is already complete).

**Approach:** Post-processing boost applied to Lucene scores after query execution.

---

## Requirements

### Functional
- Calculate recency boost based on file modification time
- Use fixed-category thresholds (≤3 days, ≤7 days, older)
- Apply aggressive multipliers: 1.5x (recent), 1.2x (week), 1.0x (older)
- Make thresholds configurable via `config.json`
- Re-rank search results with boosted scores

### Non-Functional
- Performance: Boost calculation < 1ms per result
- No database schema changes
- Backwards compatible with existing search API

---

## Architecture

### New Component: `RankingStrategy`

**Package:** `dev.notequest.search`

**Responsibility:** Calculate boost multiplier based on file age.

**Method:** `calculateBoost(LocalDateTime lastModified) → double`
- Takes file's last modified timestamp
- Returns multiplier: 1.5, 1.2, or 1.0
- Uses `java.time.temporal.ChronoUnit.DAYS` for calculation

**Constructor:** Takes `RankingConfig` dependency
```java
public RankingStrategy(RankingConfig config) {
    this.recentDays = config.getRecentDaysThreshold();
    this.recentMultiplier = config.getRecentMultiplier();
    this.weekDays = config.getWeekDaysThreshold();
    this.weekMultiplier = config.getWeekMultiplier();
    this.defaultMultiplier = config.getDefaultMultiplier();
}
```

### Modified Component: `SearchResultHandler`

**Changes:**
- Inject `RankingStrategy` dependency
- In `buildSearchResult()`: apply boost multiplier to score before returning
  ```java
  double boostedScore = score * rankingStrategy.calculateBoost(lastModified);
  ```

### Modified Component: `ConfigProvider`

**Changes:**
- Parse `ranking.recencyBoost` section from `config.json`
- Expose via getter: `getRankingConfig() → RankingConfig`

**New Class: `RankingConfig`** (data holder)
- Fields: `recentDaysThreshold`, `recentMultiplier`, `weekDaysThreshold`, `weekMultiplier`, `defaultMultiplier`
- All public (or use Lombok @Data)

### Configuration File: `config.json`

**New section:**
```json
{
  "directoryPath": "...",
  "indexPath": "...",
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

---

## Data Flow

```
SearchResultHandler.executeSearch(queryString, limit, offset)
  ├─ Execute Lucene query → TopDocs
  ├─ For each result (loop i = offset to limit):
  │  ├─ Get Document from index
  │  ├─ Extract lastModified timestamp
  │  ├─ Call buildSearchResult(doc, score, queryString)
  │  │  ├─ Extract snippet
  │  │  ├─ Get lastModified
  │  │  ├─ Calculate boost: rankingStrategy.calculateBoost(lastModified)
  │  │  ├─ Multiply score: boostedScore = score * boost
  │  │  └─ Return SearchResult with boostedScore
  │  └─ Add to results list
  └─ Return SearchResponse with re-ranked results
```

---

## Implementation Details

### RankingStrategy.calculateBoost(LocalDateTime lastModified)

```
1. Calculate days elapsed:
   daysElapsed = ChronoUnit.DAYS.between(lastModified, LocalDateTime.now())

2. Apply thresholds (aggressive):
   if (daysElapsed <= 3)     return 1.5
   else if (daysElapsed <= 7) return 1.2
   else                       return 1.0

3. Fallback: If lastModified is null/invalid, return 1.0 (no boost)
```

### SearchResultHandler.buildSearchResult() Changes

```
// Old: return SearchResult with original score
// New: apply boost before return

double boostedScore = score * rankingStrategy.calculateBoost(lastModified);

return SearchResult.builder()
    .path(path)
    .score(boostedScore)  // ← boosted score
    .lastModified(lastModified)
    .snippet(snippet)
    .build();
```

### Configuration Loading

**ConfigProvider changes:**
```
1. Parse config.json
2. Extract ranking.recencyBoost section
3. Create RankingConfig object
4. Store as instance field
5. Expose via getter: getRankingConfig()
```

---

## Testing Strategy

### Unit Tests: `RankingStrategyTest`
- Test boost calculation at exact threshold boundaries
  - Day 0: should return 1.5x
  - Day 3: should return 1.5x
  - Day 4: should return 1.2x
  - Day 7: should return 1.2x
  - Day 8: should return 1.0x
- Test with null/missing lastModified (should return 1.0x)
- Test with future dates (edge case)

### Integration Tests: `SearchResultHandlerTest`
- Verify search results are re-ranked by recency
  - Index 3 files: one 1-day old, one 5-day old, one 30-day old
  - Search query matching all 3
  - Assert results ordered: recent first (1.5x boost > 1.2x > 1.0x)

### Configuration Tests: `ConfigProviderTest`
- Verify ranking config loads from config.json
- Test with custom thresholds/multipliers
- Test missing ranking section (uses defaults)

---

## Acceptance Criteria

- ✅ RankingStrategy class implements boost calculation
- ✅ SearchResultHandler applies boost to search results
- ✅ ConfigProvider parses ranking config from config.json
- ✅ Default config.json includes ranking section
- ✅ All unit + integration tests pass
- ✅ Search results ordered by relevance (score × recency boost)
- ✅ No breaking changes to SearchResult DTO or API contract
- ✅ Performance: boost calculation < 1ms per result

---

## Risk Mitigation

| Risk | Mitigation |
|------|-----------|
| Boost values distort relevance | Start with conservative values, adjust based on user feedback |
| Missing lastModified in old indices | Fallback to 1.0x boost (no impact) |
| Config parse error | Use sensible defaults if ranking section missing |
| Performance impact | Boost is simple math (~1µs per result), negligible |

---

## Future Enhancements

- A/B test different boost multipliers
- Add file richness boost (longer files weighted higher)
- Combine multiple ranking signals (recency + size + metadata)
- Personalization (track user clicks, boost clicked results)

---

**Approved by:** User
**Ready for:** Implementation Planning
