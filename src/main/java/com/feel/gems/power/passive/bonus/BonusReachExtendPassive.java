package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Reach Extend - Extended melee and block interaction range.
 * Implementation via attribute modifier.
 */
public final class BonusReachExtendPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_REACH_EXTEND;
    }

    @Override
    public String name() {
        return "Reach Extend";
    }

    @Override
    public String description() {
        return "Extend melee and block interaction range by 2 blocks.";
    }

    @Override
    public void apply(ServerPlayerEntity player) {
        // Marker passive - implemented via attribute modifiers
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        // Marker passive
    }
}
