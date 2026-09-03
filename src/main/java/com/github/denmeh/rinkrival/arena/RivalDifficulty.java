package com.github.denmeh.rinkrival.arena;

import java.util.Locale;

/** Tuning knobs for the rival. Picked in the join GUI or {@code /rink arena <easy|normal|hard>}. */
public enum RivalDifficulty {

    EASY(8000L, 0.55, 1.45, 0.40, 2.2, 0.62, 0.30),
    NORMAL(5200L, 0.85, 0.9, 0.20, 1.45, 0.85, 0.12),
    HARD(3500L, 1.05, 0.65, 0.05, 0.5, 1.0, 0.04);

    private final long checkCooldownMs;
    private final double skateBoostMultiplier;
    private final double aimVariance;
    private final double wrongStickChance;
    /** How far off the shot line he is willing to stand, in blocks. Bigger = a hole at the far post. */
    private final double guardGap;
    private final double shotPower;
    private final double missChance;

    RivalDifficulty(long checkCooldownMs, double skateBoostMultiplier, double aimVariance,
            double wrongStickChance, double guardGap, double shotPower, double missChance) {
        this.checkCooldownMs = checkCooldownMs;
        this.skateBoostMultiplier = skateBoostMultiplier;
        this.aimVariance = aimVariance;
        this.wrongStickChance = wrongStickChance;
        this.guardGap = guardGap;
        this.shotPower = shotPower;
        this.missChance = missChance;
    }

    public long checkCooldownMs() {
        return checkCooldownMs;
    }

    public double skateBoostMultiplier() {
        return skateBoostMultiplier;
    }

    public double aimVariance() {
        return aimVariance;
    }

    public double wrongStickChance() {
        return wrongStickChance;
    }

    public double guardGap() {
        return guardGap;
    }

    public double shotPower() {
        return shotPower;
    }

    public double missChance() {
        return missChance;
    }

    public String displayName() {
        return switch (this) {
            case EASY -> "Easy";
            case NORMAL -> "Normal";
            case HARD -> "Hard";
        };
    }

    public static RivalDifficulty parse(String raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "easy", "e" -> EASY;
            case "normal", "n", "medium" -> NORMAL;
            case "hard", "h" -> HARD;
            default -> null;
        };
    }
}
