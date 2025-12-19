package com.blissmc.gems.core;

import java.util.List;

/**
 * Player-facing gem state: identity, level, and energy.
 */
public final class GemState {
    private final GemId id;
    private final GemLevel level;
    private final GemEnergyState energy;

    public GemState(GemId id, GemLevel level, GemEnergyState energy) {
        this.id = id;
        this.level = level;
        this.energy = energy;
    }

    public GemId id() {
        return id;
    }

    public GemLevel level() {
        return level;
    }

    public GemEnergyState energy() {
        return energy;
    }

    public GemState withLevel(GemLevel newLevel) {
        return new GemState(id, newLevel, energy);
    }

    public GemState withEnergy(GemEnergyState newEnergy) {
        return new GemState(id, level, newEnergy);
    }

    public GemState gainEnergy(int delta) {
        return withEnergy(energy.gain(delta));
    }

    public GemState loseEnergyOnDeath() {
        return withEnergy(energy.loseOne());
    }

    public List<String> availablePassives() {
        GemDefinition def = GemRegistry.definition(id);
        return def.availablePassives(energy);
    }

    public List<String> availableAbilities() {
        GemDefinition def = GemRegistry.definition(id);
        return def.availableAbilities(level, energy);
    }
}
