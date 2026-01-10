package com.feel.gems.power.ability.puff;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class PuffGustAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.PUFF_GUST;
    }

    @Override
    public String name() {
        return "Gust";
    }

    @Override
    public String description() {
        return "Unleash a gust that launches nearby enemies and slows them.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().puff().gustCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        var cfg = GemsBalance.v().puff();
        int radius = cfg.gustRadiusBlocks();
        if (radius <= 0) {
            player.sendMessage(Text.translatable("gems.ability.puff.gust.disabled"), true);
            return false;
        }

        if (!(player.getEntityWorld() instanceof ServerWorld world)) {
            return false;
        }
        double knockback = cfg.gustKnockback();
        double up = cfg.gustUpVelocityY();
        int slownessDuration = cfg.gustSlownessDurationTicks();
        int slownessAmplifier = cfg.gustSlownessAmplifier();
        int slowFallDuration = cfg.gustSlowFallingDurationTicks();
        int hits = 0;
        Box box = new Box(player.getBlockPos()).expand(radius);
        for (LivingEntity other : world.getEntitiesByClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
            if (other instanceof ServerPlayerEntity otherPlayer && GemTrust.isTrusted(player, otherPlayer)) {
                continue;
            }
            if (other instanceof ServerPlayerEntity otherPlayer && !VoidImmunity.canBeTargeted(player, otherPlayer)) {
                continue;
            }
            Vec3d delta = other.getEntityPos().subtract(player.getEntityPos());
            if (delta.lengthSquared() > 1.0E-4D) {
                Vec3d norm = delta.normalize();
                other.addVelocity(norm.x * knockback, up, norm.z * knockback);
                other.velocityDirty = true;
            }
            if (slownessDuration > 0) {
                other.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, slownessDuration, slownessAmplifier, true, false, false));
            }
            if (slowFallDuration > 0) {
                other.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, slowFallDuration, 0, true, false, false));
            }
            hits++;
        }

        if (slowFallDuration > 0) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, slowFallDuration, 0, true, false, false));
        }

        AbilityFeedback.burst(player, ParticleTypes.CLOUD, 30, 0.6D);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_PHANTOM_FLAP, 0.8F, 0.9F);
        player.sendMessage(Text.translatable("gems.ability.puff.gust.hit", hits), true);
        return true;
    }
}
