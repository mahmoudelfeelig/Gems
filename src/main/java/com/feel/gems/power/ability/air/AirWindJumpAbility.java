package com.feel.gems.power.ability.air;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;




public final class AirWindJumpAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.AIR_WIND_JUMP;
    }

    @Override
    public String name() {
        return "Wind Jump";
    }

    @Override
    public String description() {
        return "Launches you upward like a wind charge.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().air().windJumpCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        double up = GemsBalance.v().air().windJumpVerticalVelocity();
        double forward = GemsBalance.v().air().windJumpForwardVelocity();
        if (up <= 0.0D && forward <= 0.0D) {
            return false;
        }
        Vec3d dir = player.getRotationVec(1.0F).normalize();
        player.addVelocity(dir.x * forward, up, dir.z * forward);
        player.velocityModified = true;
        player.fallDistance = 0.0F;
        AbilityFeedback.burst(player, ParticleTypes.GUST, 18, 0.3D);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_BREEZE_WIND_BURST, 0.8F, 1.2F);
        return true;
    }
}

