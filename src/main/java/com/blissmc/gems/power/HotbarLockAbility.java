package com.blissmc.gems.power;

import com.blissmc.gems.config.GemsBalance;
import com.blissmc.gems.trust.GemTrust;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class HotbarLockAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.HOTBAR_LOCK;
    }

    @Override
    public String name() {
        return "Hotbar Lock";
    }

    @Override
    public String description() {
        return "Hotbar Lock: locks an enemy to their current hotbar slot for a short time.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().wealth().hotbarLockCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        LivingEntity target = Targeting.raycastLiving(player, GemsBalance.v().wealth().hotbarLockRangeBlocks());
        if (!(target instanceof ServerPlayerEntity other)) {
            player.sendMessage(Text.literal("No player target."), true);
            return true;
        }
        if (GemTrust.isTrusted(player, other)) {
            player.sendMessage(Text.literal("Target is trusted."), true);
            return true;
        }

        HotbarLock.lock(other, other.getInventory().selectedSlot, GemsBalance.v().wealth().hotbarLockDurationTicks());
        AbilityFeedback.sound(player, SoundEvents.BLOCK_CHAIN_PLACE, 0.8F, 1.1F);
        AbilityFeedback.burstAt(other.getServerWorld(), other.getPos().add(0.0D, 1.0D, 0.0D), ParticleTypes.CRIT, 12, 0.2D);
        player.sendMessage(Text.literal("Hotbar locked."), true);
        return true;
    }
}
