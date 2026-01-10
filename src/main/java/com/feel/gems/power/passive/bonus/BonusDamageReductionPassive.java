package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Damage Reduction - Permanent resistance effect.
 */
public final class BonusDamageReductionPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_DAMAGE_REDUCTION;
    }

    @Override
    public String name() {
        return "Damage Reduction";
    }

    @Override
    public String description() {
        return "Permanent Resistance I effect (20% damage reduction).";
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
