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

        // Handle future dates (negative daysElapsed) as no boost
        if (daysElapsed < 0) {
            return config.getDefaultMultiplier();
        }

        if (daysElapsed <= config.getRecentDaysThreshold()) {
            return config.getRecentMultiplier();
        } else if (daysElapsed <= config.getWeekDaysThreshold()) {
            return config.getWeekMultiplier();
        } else {
            return config.getDefaultMultiplier();
        }
    }
}
