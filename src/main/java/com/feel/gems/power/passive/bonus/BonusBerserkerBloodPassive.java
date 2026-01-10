package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Berserker Blood - Attack speed increases as health decreases.
 * Implementation via tick handler that applies Haste based on health.
 */
public final class BonusBerserkerBloodPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_BERSERKER_BLOOD;
    }

    @Override
    public String name() {
        return "Berserker Blood";
    }

    @Override
    public String description() {
        return "Gain attack speed as your health decreases (up to Haste III at 25% HP).";
    }

    @Override
    public void apply(ServerPlayerEntity player) {
        // Marker passive - implemented via tick handler
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        // Remove any lingering haste effect from this passive
    }
}
