package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Thick Skin - Reduced damage from all sources.
 * Implementation via event hook.
 */
public final class BonusThickSkinPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_THICK_SKIN;
    }

    @Override
    public String name() {
        return "Thick Skin";
    }

    @Override
    public String description() {
        return "Reduced damage from all sources.";
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
