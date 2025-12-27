package com.feel.gems.core;




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
        // Ability unlocks start at energy 2.
        return energy > 1;
    }

    /**
     * Ability unlock progression:
     * - Energy 0-1: no abilities.
     * - Energy 2-4: unlock abilities in order, one per energy (energy-1 abilities).
     * - Energy 5+: unlock all remaining abilities at once; overflow keeps all abilities.
     */
    public int unlockedAbilityCount(int abilityCount) {
        if (abilityCount <= 0) {
            return 0;
        }
        if (energy <= 1) {
            return 0;
        }
        if (energy <= 4) {
            return Math.min(energy - 1, abilityCount);
        }
        return abilityCount;
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
