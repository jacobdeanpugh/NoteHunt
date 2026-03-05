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
