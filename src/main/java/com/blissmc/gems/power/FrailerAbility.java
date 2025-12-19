package com.blissmc.gems.power;

import com.blissmc.gems.config.GemsBalance;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import com.blissmc.gems.trust.GemTrust;

public final class FrailerAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.FRAILER;
    }

    @Override
    public String name() {
        return "Frailer";
    }

    @Override
    public String description() {
        return "Applies Weakness to a targeted enemy.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().strength().frailerCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        var target = Targeting.raycastLiving(player, GemsBalance.v().strength().frailerRangeBlocks());
        if (target == null) {
            player.sendMessage(Text.literal("No target."), true);
            return true;
        }
        if (target instanceof ServerPlayerEntity other && GemTrust.isTrusted(player, other)) {
            player.sendMessage(Text.literal("Target is trusted."), true);
            return true;
        }

        target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, GemsBalance.v().strength().frailerDurationTicks(), 0));
        AbilityFeedback.sound(player, SoundEvents.ENTITY_WITHER_SHOOT, 0.7F, 1.3F);
        if (player.getServerWorld() != null) {
            AbilityFeedback.burstAt(player.getServerWorld(), target.getPos().add(0.0D, 1.0D, 0.0D), ParticleTypes.ASH, 14, 0.25D);
        }
        player.sendMessage(Text.literal("Frailer: weakened " + target.getName().getString()), true);
        return true;
    }
}
