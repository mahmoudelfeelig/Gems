package com.feel.gems.power.ability.speed;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.runtime.AbilityRuntime;
import com.feel.gems.power.gem.speed.SpeedMomentum;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;


public final class SpeedSlipstreamAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SPEED_SLIPSTREAM;
    }

    @Override
    public String name() {
        return "Slipstream";
    }

    @Override
    public String description() {
        return "Creates a wind lane ahead that boosts trusted allies and hinders enemies.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().speed().slipstreamCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        float momentum = SpeedMomentum.multiplier(player);
        int duration = Math.max(1, Math.round(GemsBalance.v().speed().slipstreamDurationTicks() * momentum));
        Vec3d dir = player.getRotationVec(1.0F);

        AbilityRuntime.startSpeedSlipstream(player, dir, duration, momentum);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_BREEZE_WIND_BURST, 0.8F, 1.3F);
        if (!(player.getEntityWorld() instanceof ServerWorld world)) {
            player.sendMessage(Text.literal("Slipstream active."), true);
            return true;
        }
        AbilityFeedback.beam(
                world,
                player.getEntityPos().add(0.0D, 0.8D, 0.0D),
                player.getEntityPos().add(dir.x * 6.0D, 0.8D, dir.z * 6.0D),
                ParticleTypes.CLOUD,
                12
        );
        player.sendMessage(Text.literal("Slipstream active."), true);
        return true;
    }
}
