package dev.notequest.config;

import org.junit.jupiter.api.Test;
import dev.notequest.util.ConfigProvider;
import static org.junit.jupiter.api.Assertions.*;

public class ConfigProviderTest {

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
}
