package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Critical Strike - Chance for attacks to deal double damage.
 * Implementation via mixin/event hook checking for this passive.
 */
public final class BonusCriticalStrikePassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_CRITICAL_STRIKE;
    }

    @Override
    public String name() {
        return "Critical Strike";
    }

    @Override
    public String description() {
        return "20% chance for melee attacks to deal double damage.";
    }

    @Override
    public void apply(ServerPlayerEntity player) {
        // Marker passive - implemented via event hooks
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        // Marker passive
    }
}
