package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Regeneration Boost - Enhanced natural regeneration.
 */
public final class BonusRegenerationBoostPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_REGENERATION_BOOST;
    }

    @Override
    public String name() {
        return "Regeneration Boost";
    }

    @Override
    public String description() {
        return "Permanent Regeneration I effect.";
    }

    @Override
    public void apply(ServerPlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, Integer.MAX_VALUE, 0, false, false, true));
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        player.removeStatusEffect(StatusEffects.REGENERATION);
    }
}
