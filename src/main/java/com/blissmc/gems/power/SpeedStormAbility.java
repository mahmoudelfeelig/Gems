package com.feel.gems.power;

import com.feel.gems.config.GemsBalance;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class SpeedStormAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SPEED_STORM;
    }

    @Override
    public String name() {
        return "Speed Storm";
    }

    @Override
    public String description() {
        return "Creates a field that buffs trusted players and heavily slows enemies.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().speed().speedStormCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        float momentum = SpeedMomentum.multiplier(player);
        int duration = Math.max(1, Math.round(GemsBalance.v().speed().speedStormDurationTicks() * momentum));
        AbilityRuntime.startSpeedStorm(player, duration, momentum);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_BREEZE_WIND_BURST, 0.9F, 1.0F);
        AbilityFeedback.burst(player, ParticleTypes.CLOUD, 20, 0.45D);
        player.sendMessage(Text.literal("Speed Storm active."), true);
        return true;
    }
}
