package com.blissmc.gems.power;

import com.blissmc.gems.config.GemsBalance;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
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
        int duration = GemsBalance.v().speed().terminalVelocityDurationTicks();
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration, GemsBalance.v().speed().terminalVelocitySpeedAmplifier(), true, false, false));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, duration, GemsBalance.v().speed().terminalVelocityHasteAmplifier(), true, false, false));
        player.sendMessage(Text.literal("Terminal Velocity!"), true);
        return true;
    }
}
