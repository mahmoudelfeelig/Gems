package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * XP Boost - Gain bonus XP from all sources.
 * Implementation via event hook.
 */
public final class BonusXpBoostPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_XP_BOOST;
    }

    @Override
    public String name() {
        return "XP Boost";
    }

    @Override
    public String description() {
        return "Gain 25% bonus XP from all sources.";
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
