package com.github.denmeh.npcaitest.arena;

/** Tuning knobs for the rival. Defaults to {@link #NORMAL} until a UI exists. */
public enum RivalDifficulty {

    EASY(7000L, 0.85, 1.15, 0.30),
    NORMAL(5200L, 1.0, 0.9, 0.20),
    HARD(3500L, 1.15, 0.65, 0.05);

    private final long checkCooldownMs;
    private final double skateBoostMultiplier;
    private final double aimVariance;
    private final double wrongStickChance;

    RivalDifficulty(long checkCooldownMs, double skateBoostMultiplier, double aimVariance, double wrongStickChance) {
        this.checkCooldownMs = checkCooldownMs;
        this.skateBoostMultiplier = skateBoostMultiplier;
        this.aimVariance = aimVariance;
        this.wrongStickChance = wrongStickChance;
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
}
