package com.feel.gems.power.passive.strength;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemMaintainedPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class StrengthAdrenalinePassive implements GemMaintainedPassive {
    @Override
    public Identifier id() {
        return PowerIds.STRENGTH_ADRENALINE;
    }

    @Override
    public String name() {
        return "Adrenaline";
    }

    @Override
    public String description() {
        return "Gain brief resistance when critically injured.";
    }

    @Override
    public void apply(ServerPlayerEntity player) {
        maintain(player);
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        player.removeStatusEffect(StatusEffects.RESISTANCE);
    }

    @Override
    public void maintain(ServerPlayerEntity player) {
        var cfg = GemsBalance.v().strength();
        float threshold = cfg.adrenalineThresholdHearts() * 2.0F;
        if (player.getHealth() > threshold) {
            return;
        }
        int duration = cfg.adrenalineDurationTicks();
        int amplifier = cfg.adrenalineResistanceAmplifier();
        if (duration > 0) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, duration, amplifier, true, false, false));
        }
    }
}
