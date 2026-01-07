package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Magnetic Pull - Items and XP are attracted from farther away.
 * Implementation via tick handler that moves items toward player.
 */
public final class BonusMagneticPullPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_MAGNETIC_PULL;
    }

    @Override
    public String name() {
        return "Magnetic Pull";
    }

    @Override
    public String description() {
        return "Items and XP orbs are attracted from 8 blocks away.";
    }

    @Override
    public void apply(ServerPlayerEntity player) {
        // Marker passive - implemented via tick handler
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        // Marker passive
    }
}
