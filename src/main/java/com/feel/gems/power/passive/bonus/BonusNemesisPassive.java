package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Nemesis - Deal 25% more damage to the last player who killed you.
 * Implementation via death event (store killer) and damage event hooks.
 */
public final class BonusNemesisPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_NEMESIS;
    }

    @Override
    public String name() {
        return "Nemesis";
    }

    @Override
    public String description() {
        return "Deal 25% more damage to the last player who killed you.";
    }

    @Override
    public void apply(ServerPlayerEntity player) {
        // Marker passive - uses persistent data for nemesis tracking
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        // Keep nemesis data even when passive removed
    }
}
