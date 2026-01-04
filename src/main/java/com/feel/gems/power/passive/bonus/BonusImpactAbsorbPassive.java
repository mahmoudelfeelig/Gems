package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Impact Absorb - Absorb incoming damage.
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
        return "Permanent Resistance I effect for damage absorption.";
    }

    @Override
    public void apply(ServerPlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, Integer.MAX_VALUE, 0, false, false, true));
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        player.removeStatusEffect(StatusEffects.RESISTANCE);
    }
}
