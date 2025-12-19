package com.blissmc.gems.core;

/**
 * Normalized energy tiers for gems. Values are clamped between 0 and 10.
 * Naming follows the Legendary → Broken ladder with Legendary +1…+5 overflow.
 */
public enum GemEnergyTier {
    BROKEN(0),
    COMMON(1),
    RARE(2),
    ELITE(3),
    MYTHICAL(4),
    LEGENDARY(5),
    LEGENDARY_PLUS_1(6),
    LEGENDARY_PLUS_2(7),
    LEGENDARY_PLUS_3(8),
    LEGENDARY_PLUS_4(9),
    LEGENDARY_PLUS_5(10);

    private final int energy;

    GemEnergyTier(int energy) {
        this.energy = energy;
    }

    public int energy() {
        return energy;
    }

    public boolean isBroken() {
        return this == BROKEN;
    }

    public static GemEnergyTier fromEnergy(int rawEnergy) {
        int clamped = Math.max(0, Math.min(10, rawEnergy));
        for (GemEnergyTier tier : values()) {
            if (tier.energy == clamped) {
                return tier;
            }
        }
        return BROKEN;
    }
}
