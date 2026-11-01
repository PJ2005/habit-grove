package com.example.grove.domain;

public enum TreeHealth {
    THRIVING, STABLE, WILTING, DYING, DEAD;

    public static TreeHealth fromRolling(double rolling) {
        if (rolling >= 70) return THRIVING;
        if (rolling >= 40) return STABLE;
        if (rolling >= 20) return WILTING;
        return DYING;   // DEAD is set only by the 7-day lifecycle rule, not by one score
    }
}
