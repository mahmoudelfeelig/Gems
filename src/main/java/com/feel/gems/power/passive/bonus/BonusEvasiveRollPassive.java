package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Evasive Roll - Chance to dodge incoming attacks.
 * Implementation via event hook.
 */
public final class BonusEvasiveRollPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_EVASIVE_ROLL;
    }

    @Override
    public String name() {
        return "Evasive Roll";
    }

    @Override
    public String description() {
        return "Chance to dodge incoming attacks with an evasive roll.";
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
