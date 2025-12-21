package com.feel.gems.power;

import com.feel.gems.config.GemsBalance;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class SpeedAfterimageAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SPEED_AFTERIMAGE;
    }

    @Override
    public String name() {
        return "Afterimage";
    }

    @Override
    public String description() {
        return "Briefly vanish with a speed burst; the first hit breaks the effect.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().speed().afterimageCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        float momentum = SpeedMomentum.multiplier(player);
        int duration = Math.max(1, Math.round(GemsBalance.v().speed().afterimageDurationTicks() * momentum));
        AbilityRuntime.startSpeedAfterimage(player, duration);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 0.7F, 1.5F);
        AbilityFeedback.burst(player, ParticleTypes.CLOUD, 14, 0.35D);
        player.sendMessage(Text.literal("Afterimage active."), true);
        return true;
    }
}
