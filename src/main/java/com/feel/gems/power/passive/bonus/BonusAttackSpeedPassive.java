package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Attack Speed - Permanent haste for faster swings.
 */
public final class BonusAttackSpeedPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_ATTACK_SPEED;
    }

    @Override
    public String name() {
        return "Attack Speed";
    }

    @Override
    public String description() {
        return "Permanent Haste II effect for faster attack speed.";
    }

    @Override
    public void apply(ServerPlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, Integer.MAX_VALUE, 1, false, false, true));
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        player.removeStatusEffect(StatusEffects.HASTE);
    }
}
