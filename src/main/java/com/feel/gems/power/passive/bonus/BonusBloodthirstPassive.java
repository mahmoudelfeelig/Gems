package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Bloodthirst - Kills restore 2 hearts.
 * Implementation via kill event hook.
 */
public final class BonusBloodthirstPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_BLOODTHIRST;
    }

    @Override
    public String name() {
        return "Bloodthirst";
    }

    @Override
    public String description() {
        return "Killing an enemy restores 2 hearts.";
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
