package com.blissmc.gems.core;

/**
 * Represents gem level progression. Levels are clamped between 1 and 10.
 */
public final class GemLevel {
    private final int level;

    public GemLevel(int level) {
        this.level = clamp(level);
    }

    public int value() {
        return level;
    }

    public GemLevel increase(int delta) {
        if (delta == 0) {
            return this;
        }
        return new GemLevel(level + delta);
    }

    /**
     * Ability unlock progression:
     * - Level 1: passives only.
     * - Levels 2-4: unlock abilities in order, one per level.
     * - Level 5: unlock all remaining abilities at once.
     * - Levels 6-10: no extra unlocks; buffer only.
     */
    public int unlockedAbilityCount(int abilityCount) {
        if (abilityCount <= 0) {
            return 0;
        }
        if (level <= 1) {
            return 0;
        }
        if (level >= 5) {
            return abilityCount;
        }
        int unlocked = level - 1; // levels 2-4
        return Math.min(unlocked, abilityCount);
    }

    private static int clamp(int value) {
        return Math.max(1, Math.min(10, value));
    }
}
