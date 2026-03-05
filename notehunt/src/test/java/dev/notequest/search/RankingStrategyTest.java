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
