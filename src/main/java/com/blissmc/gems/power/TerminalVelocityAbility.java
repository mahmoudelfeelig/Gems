package com.blissmc.gems.power;

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
        return 30 * 20;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        int duration = 10 * 20;
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration, 2, true, false, false));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, duration, 1, true, false, false));
        player.sendMessage(Text.literal("Terminal Velocity!"), true);
        return true;
    }
}

