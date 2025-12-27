package com.feel.gems.power.ability.life;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.runtime.AbilityRuntime;
import com.feel.gems.power.util.Targeting;
import com.feel.gems.trust.GemTrust;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;



public final class HeartLockAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.HEART_LOCK;
    }

    @Override
    public String name() {
        return "Heart Lock";
    }

    @Override
    public String description() {
        return "Temporarily locks an enemy player's max health to their current health.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().life().heartLockCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        var target = Targeting.raycastLiving(player, GemsBalance.v().life().heartLockRangeBlocks());
        if (!(target instanceof ServerPlayerEntity other)) {
            player.sendMessage(Text.literal("No player target."), true);
            return false;
        }
        if (GemTrust.isTrusted(player, other)) {
            player.sendMessage(Text.literal("Target is trusted."), true);
            return false;
        }

        AbilityRuntime.startHeartLock(player, other, GemsBalance.v().life().heartLockDurationTicks());
        AbilityFeedback.sound(player, SoundEvents.BLOCK_CHAIN_PLACE, 0.8F, 0.9F);
        AbilityFeedback.burstAt(other.getServerWorld(), other.getPos().add(0.0D, 1.0D, 0.0D), ParticleTypes.ENCHANT, 18, 0.25D);
        player.sendMessage(Text.literal("Heart Lock applied."), true);
        return true;
    }
}
