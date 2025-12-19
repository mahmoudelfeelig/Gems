package com.blissmc.gems.core;

import java.util.Collections;
import java.util.List;

/**
 * Static definition of a gem: ordered abilities and passives.
 */
public final class GemDefinition {
    private final GemId id;
    private final List<String> passives;
    private final List<String> abilities; // ordered unlock list

    public GemDefinition(GemId id, List<String> passives, List<String> abilities) {
        this.id = id;
        this.passives = List.copyOf(passives);
        this.abilities = List.copyOf(abilities);
    }

    public GemId id() {
        return id;
    }

    public List<String> passives() {
        return passives;
    }

    public List<String> abilities() {
        return abilities;
    }

    public List<String> availablePassives(GemEnergyState energy) {
        if (!energy.canUsePassives()) {
            return List.of();
        }
        return passives;
    }

    public List<String> availableAbilities(GemLevel level, GemEnergyState energy) {
        if (!energy.canUseAbilities() || abilities.isEmpty()) {
            return List.of();
        }
        int unlocked = level.unlockedAbilityCount(abilities.size());
        int allowedByEnergy = Math.min(unlocked, energy.abilityBudget());
        if (allowedByEnergy <= 0) {
            return List.of();
        }
        return Collections.unmodifiableList(abilities.subList(0, allowedByEnergy));
    }
}
