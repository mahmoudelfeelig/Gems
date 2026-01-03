package com.feel.gems.power.ability.air;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;




public final class AirDashAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.AIR_DASH;
    }

    @Override
    public String name() {
        return "Air Dash";
    }

    @Override
    public String description() {
        return "Dashes forward with a brief i-frame window.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().air().dashCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        double velocity = GemsBalance.v().air().dashVelocity();
        double up = GemsBalance.v().air().dashUpVelocity();
        int iFrame = GemsBalance.v().air().dashIFrameDurationTicks();
        int amp = GemsBalance.v().air().dashIFrameResistanceAmplifier();
        if (velocity <= 0.0D) {
            return false;
        }

        Vec3d dir = player.getRotationVec(1.0F).normalize();
        player.addVelocity(dir.x * velocity, up, dir.z * velocity);
        player.velocityDirty = true;
        if (iFrame > 0) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, iFrame, amp, true, false, false));
        }

        AbilityFeedback.beam(player.getEntityWorld(),
                player.getEntityPos().add(0.0D, 1.0D, 0.0D),
                player.getEntityPos().add(dir.multiply(3.0D)).add(0.0D, 1.0D, 0.0D),
                ParticleTypes.CLOUD,
                12);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_ENDER_DRAGON_FLAP, 0.7F, 1.4F);
        return true;
    }
}

