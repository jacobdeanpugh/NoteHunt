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
