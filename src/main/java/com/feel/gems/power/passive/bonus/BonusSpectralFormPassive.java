package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Mist Form - Chance to phase through attacks.
 * Implementation via damage hook.
 */
public final class BonusSpectralFormPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_SPECTRAL_FORM;
    }

    @Override
    public String name() {
        return "Mist Form";
    }

    @Override
    public String description() {
        return "10% chance to phase through attacks.";
    }

    @Override
    public void apply(ServerPlayerEntity player) {
        // Marker passive - implemented via damage hooks
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        // Marker passive
    }
}
