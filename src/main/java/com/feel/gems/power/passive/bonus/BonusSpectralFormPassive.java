package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Spectral Form - Become semi-transparent; mobs have 20% chance to lose aggro.
 * Implementation via tick handler for mob AI manipulation.
 */
public final class BonusSpectralFormPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_SPECTRAL_FORM;
    }

    @Override
    public String name() {
        return "Spectral Form";
    }

    @Override
    public String description() {
        return "Become semi-transparent. Mobs have 20% chance to lose aggro each second.";
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
