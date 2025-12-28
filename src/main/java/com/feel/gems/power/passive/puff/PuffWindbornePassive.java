package com.feel.gems.power.passive.puff;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemMaintainedPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class PuffWindbornePassive implements GemMaintainedPassive {
    @Override
    public Identifier id() {
        return PowerIds.PUFF_WINDBORNE;
    }

    @Override
    public String name() {
        return "Windborne";
    }

    @Override
    public String description() {
        return "While airborne, you gain slow falling.";
    }

    @Override
    public void apply(ServerPlayerEntity player) {
        maintain(player);
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        player.removeStatusEffect(StatusEffects.SLOW_FALLING);
    }

    @Override
    public void maintain(ServerPlayerEntity player) {
        if (player.isOnGround()) {
            return;
        }
        var cfg = GemsBalance.v().puff();
        int duration = cfg.windborneDurationTicks();
        int amplifier = cfg.windborneSlowFallingAmplifier();
        if (duration > 0) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, duration, amplifier, true, false, false));
        }
    }
}
