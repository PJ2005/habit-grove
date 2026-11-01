package com.example.grove.domain;

import java.util.List;

public final class ScoringRules {

    public static final int DAILY_TARGET = 10;
    public static final int NEGATIVE_MULTIPLIER = 2;
    public static final double SMOOTHING = 0.3;
    public static final int DEATH_STREAK_DAYS = 7;
    public static final double DEATH_THRESHOLD = 10.0;

    private ScoringRules() {}

    /** Daily score from one day's totals, clamped to 0..100. */
    public static int dailyScore(int positivePoints, int negativePoints) {
        int raw = 100 * (positivePoints - NEGATIVE_MULTIPLIER * negativePoints) / DAILY_TARGET;
        return Math.max(0, Math.min(100, raw));
    }

    /** Exponentially smoothed rolling score. */
    public static double rolling(double previousRolling, int todayScore) {
        return SMOOTHING * todayScore + (1 - SMOOTHING) * previousRolling;
    }

    /**
     * True when the newest DEATH_STREAK_DAYS rolling scores are all below the threshold.
     * The list must be ordered newest first. Fewer than 7 values is never a death.
     */
    public static boolean isDeathStreak(List<Double> rollingNewestFirst) {
        if (rollingNewestFirst.size() < DEATH_STREAK_DAYS) {
            return false;
        }
        return rollingNewestFirst.subList(0, DEATH_STREAK_DAYS).stream()
                .allMatch(r -> r < DEATH_THRESHOLD);
    }
}
