package com.example.grove.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScoringRulesTest {

    @Test
    void fullTargetDayScores100() {
        assertEquals(100, ScoringRules.dailyScore(10, 0));
    }

    @Test
    void negativePointsCountDouble() {
        // 10 positive minus 2×2 negative = 6 net → 60
        assertEquals(60, ScoringRules.dailyScore(10, 2));
    }

    @Test
    void dailyScoreNeverGoesBelowZero() {
        assertEquals(0, ScoringRules.dailyScore(0, 20));
    }

    @Test
    void rollingMovesGraduallyTowardToday() {
        double rolling = ScoringRules.rolling(100, 0);
        assertEquals(70.0, rolling, 0.0001);
    }

    @Test
    void healthLevelsMatchThresholds() {
        assertEquals(TreeHealth.THRIVING, TreeHealth.fromRolling(85));
        assertEquals(TreeHealth.WILTING, TreeHealth.fromRolling(30));
        assertEquals(TreeHealth.DYING, TreeHealth.fromRolling(12));
    }
}
