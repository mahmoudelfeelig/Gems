package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Adrenaline Rush - Gain brief Speed III after getting a kill.
 * Implementation via kill event hook.
 */
public final class BonusAdrenalineRushPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_ADRENALINE_RUSH;
    }

    @Override
    public String name() {
        return "Adrenaline Rush";
    }

    @Override
    public String description() {
        return "Gain Speed III for 5 seconds after killing an enemy.";
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
