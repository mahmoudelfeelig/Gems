package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Hunger Resist - Permanent saturation effect to slow hunger.
 */
public final class BonusHungerResistPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_HUNGER_RESIST;
    }

    @Override
    public String name() {
        return "Hunger Resist";
    }

    @Override
    public String description() {
        return "Permanent Saturation effect to slow hunger drain.";
    }

    @Override
    public void apply(ServerPlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, Integer.MAX_VALUE, 0, false, false, true));
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        player.removeStatusEffect(StatusEffects.SATURATION);
    }
}
