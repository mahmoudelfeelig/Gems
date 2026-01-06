package com.feel.gems.power.ability.air;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
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

public final class AirCrosswindAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.AIR_CROSSWIND;
    }

    @Override
    public String name() {
        return "Crosswind";
    }

    @Override
    public String description() {
        return "Send a cutting gust forward that knocks back and slows enemies.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().air().crosswindCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        var cfg = GemsBalance.v().air();
        int range = cfg.crosswindRangeBlocks();
        int radius = cfg.crosswindRadiusBlocks();
        if (range <= 0 || radius <= 0) {
            player.sendMessage(Text.translatable("gems.message.ability_disabled_server"), true);
            return false;
        }

        Vec3d dir = player.getRotationVec(1.0F).normalize();
        Vec3d start = player.getEntityPos().add(0.0D, 1.0D, 0.0D);
        Vec3d end = start.add(dir.multiply(range));
        Box box = new Box(start, end).expand(radius);

        ServerWorld world = player.getEntityWorld();
        float damage = cfg.crosswindDamage();
        double knockback = cfg.crosswindKnockback();
        int slowDuration = cfg.crosswindSlownessDurationTicks();
        int slowAmp = cfg.crosswindSlownessAmplifier();
        int hits = 0;
        for (LivingEntity other : world.getEntitiesByClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
            if (other instanceof ServerPlayerEntity otherPlayer && GemTrust.isTrusted(player, otherPlayer)) {
                continue;
            }
            other.damage(world, player.getDamageSources().playerAttack(player), damage);
            if (knockback > 0.0D) {
                other.addVelocity(dir.x * knockback, 0.1D, dir.z * knockback);
                other.velocityDirty = true;
            }
            if (slowDuration > 0) {
                other.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, slowDuration, slowAmp, true, false, false));
            }
            hits++;
        }

        AbilityFeedback.ring(world, player.getEntityPos().add(0.0D, 0.2D, 0.0D), Math.min(range, 12), ParticleTypes.GUST, 24);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_BREEZE_WIND_BURST, 0.9F, 1.1F);
        player.sendMessage(Text.translatable("gems.ability.air.crosswind.hit", hits), true);
        return true;
    }
}
