package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class BonusTimewarpAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_TIMEWARP;
    }

    @Override
    public String name() {
        return "Timewarp";
    }

    @Override
    public String description() {
        return "Drastically increase your speed for a short duration.";
    }

    @Override
    public int cooldownTicks() {
        return 800; // 40 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 100, 4));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 100, 2));
        return true;
    }
}
