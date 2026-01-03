package com.feel.gems.power.gem.terror;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemMaintainedPassive;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;


public final class TerrorDreadAuraPassive implements GemMaintainedPassive {
    @Override
    public Identifier id() {
        return PowerIds.TERROR_DREAD_AURA;
    }

    @Override
    public String name() {
        return "Dread Aura";
    }

    @Override
    public String description() {
        return "Untrusted players near you are afflicted with Darkness.";
    }

    @Override
    public void apply(ServerPlayerEntity player) {
        maintain(player);
    }

    @Override
    public void maintain(ServerPlayerEntity player) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        int radius = GemsBalance.v().terror().dreadAuraRadiusBlocks();
        if (radius <= 0) {
            return;
        }

        int duration = 60;
        int amp = GemsBalance.v().terror().dreadAuraAmplifier();
        for (ServerPlayerEntity other : world.getPlayers(p -> p != player && p.squaredDistanceTo(player) <= radius * (double) radius)) {
            if (GemTrust.isTrusted(player, other)) {
                continue;
            }
            other.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, duration, amp, true, false, false));
        }
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        // No-op: Darkness expires naturally and should not be forcibly removed from others.
    }
}

