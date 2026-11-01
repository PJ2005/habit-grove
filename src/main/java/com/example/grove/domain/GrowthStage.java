package com.example.grove.domain;

public enum GrowthStage {
    SEEDLING(0), SAPLING(50), YOUNG(150), MATURE(400), ANCIENT(1000);

    private final int minGrowthPoints;

    GrowthStage(int minGrowthPoints) {
        this.minGrowthPoints = minGrowthPoints;
    }

    public static GrowthStage fromGrowthPoints(int growthPoints) {
        GrowthStage result = SEEDLING;
        for (GrowthStage stage : values()) {
            if (growthPoints >= stage.minGrowthPoints) result = stage;
        }
        return result;
    }

    /** Leaves scale with growth stage and current rolling health (0..100). */
    public int leavesFor(double rolling) {
        int maxLeaves = (ordinal() + 1) * 20;
        return (int) Math.round(maxLeaves * Math.max(0, Math.min(100, rolling)) / 100.0);
    }
}
