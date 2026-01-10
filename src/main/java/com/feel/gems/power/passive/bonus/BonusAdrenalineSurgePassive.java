package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Adrenaline Surge - Boost movement and attack speed.
 */
public final class BonusAdrenalineSurgePassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_ADRENALINE_SURGE;
    }

    @Override
    public String name() {
        return "Adrenaline Surge";
    }

    @Override
    public String description() {
        return "Permanent Speed I and Haste I effects for enhanced combat.";
    }

    @Override
    public void apply(ServerPlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, Integer.MAX_VALUE, 0, false, false, true));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, Integer.MAX_VALUE, 0, false, false, true));
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        player.removeStatusEffect(StatusEffects.SPEED);
        player.removeStatusEffect(StatusEffects.HASTE);
    }
}
