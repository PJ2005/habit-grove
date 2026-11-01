package com.example.grove.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeathStreakTest {

    @Test
    void sevenLowDaysIsDeath() {
        List<Double> rolling = List.of(0.0, 2.0, 4.0, 5.0, 6.0, 7.0, 8.0);
        assertTrue(ScoringRules.isDeathStreak(rolling));
    }

    @Test
    void sixLowDaysIsNotDeath() {
        List<Double> rolling = List.of(0.0, 2.0, 4.0, 5.0, 6.0, 7.0);
        assertFalse(ScoringRules.isDeathStreak(rolling));
    }

    @Test
    void oneHighDayBreaksTheStreak() {
        // Newest first: the high score is inside the 7-day window
        List<Double> rolling = List.of(0.0, 2.0, 4.0, 15.0, 6.0, 7.0, 8.0);
        assertFalse(ScoringRules.isDeathStreak(rolling));
    }

    @Test
    void highOlderDayDoesNotBreakStreak() {
        // Only the newest 7 count
        List<Double> rolling = List.of(0.0, 2.0, 4.0, 5.0, 6.0, 7.0, 8.0, 90.0);
        assertTrue(ScoringRules.isDeathStreak(rolling));
    }
}
