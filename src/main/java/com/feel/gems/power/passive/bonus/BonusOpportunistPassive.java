package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Opportunist - Deal bonus damage to enemies attacking others.
 * Implementation via attack event hook that checks target's last attacker.
 */
public final class BonusOpportunistPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_OPPORTUNIST;
    }

    @Override
    public String name() {
        return "Opportunist";
    }

    @Override
    public String description() {
        return "Deal 25% more damage to enemies focused on someone else.";
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
