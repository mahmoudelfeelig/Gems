package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Poison Immunity - Immune to poison damage.
 * Implementation via event hook.
 */
public final class BonusPoisonImmunityPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_POISON_IMMUNITY;
    }

    @Override
    public String name() {
        return "Poison Immunity";
    }

    @Override
    public String description() {
        return "Immune to poison and wither effects.";
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
