package com.feel.gems.power;

import com.feel.gems.config.GemsBalance;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class TerminalVelocityAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.TERMINAL_VELOCITY;
    }

    @Override
    public String name() {
        return "Terminal Velocity";
    }

    @Override
    public String description() {
        return "Temporarily grants Speed III and Haste II.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().speed().terminalVelocityCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        float momentum = SpeedMomentum.multiplier(player);
        int duration = Math.max(1, Math.round(GemsBalance.v().speed().terminalVelocityDurationTicks() * momentum));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration, GemsBalance.v().speed().terminalVelocitySpeedAmplifier(), true, false, false));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, duration, GemsBalance.v().speed().terminalVelocityHasteAmplifier(), true, false, false));
        AbilityFeedback.sound(player, SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.9F, 1.3F);
        AbilityFeedback.burst(player, ParticleTypes.CLOUD, 14, 0.35D);
        player.sendMessage(Text.literal("Terminal Velocity!"), true);
        return true;
    }
}
