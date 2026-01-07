package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Impact Absorb - Convert damage taken into temporary absorption hearts.
 */
public final class BonusImpactAbsorbPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_IMPACT_ABSORB;
    }

    @Override
    public String name() {
        return "Impact Absorb";
    }

    @Override
    public String description() {
        return "Convert 20% of damage taken into temporary absorption hearts.";
    }

    @Override
    public void apply(ServerPlayerEntity player) {
        // Marker passive - implemented via event hooks in damage handler
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        // Marker passive
    }
}
