package com.blissmc.gems.core;

/**
 * Tracks a player's current gem energy. Immutable so transitions are explicit.
 */
public final class GemEnergyState {
    private final int energy;

    public GemEnergyState(int energy) {
        this.energy = clamp(energy);
    }

    public int value() {
        return energy;
    }

    public GemEnergyTier tier() {
        return GemEnergyTier.fromEnergy(energy);
    }

    public boolean canUsePassives() {
        return energy > 0;
    }

    public boolean canUseAbilities() {
        return energy > 0;
    }

    /**
     * Ability budget scales down as energy drops: budget = max(0, energy - 1).
     */
    public int abilityBudget() {
        return Math.max(0, energy - 1);
    }

    public GemEnergyState gain(int delta) {
        if (delta == 0) {
            return this;
        }
        return new GemEnergyState(energy + delta);
    }

    public GemEnergyState loseOne() {
        if (energy == 0) {
            return this;
        }
        return new GemEnergyState(energy - 1);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(10, value));
    }
}
