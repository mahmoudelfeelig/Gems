package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/**
 * Tidal Wave - Summon a wave that knocks back and slows enemies.
 */
public final class BonusTidalWaveAbility implements GemAbility {
    private static final double RANGE = 12.0;
    private static final double KNOCKBACK = 2.0;

    @Override
    public Identifier id() {
        return PowerIds.BONUS_TIDAL_WAVE;
    }

    @Override
    public String name() {
        return "Tidal Wave";
    }

    @Override
    public String description() {
        return "Send a tidal wave forward, knocking back and slowing enemies.";
    }

    @Override
    public int cooldownTicks() {
        return 300; // 15 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) return false;
        Vec3d direction = player.getRotationVector().multiply(1, 0, 1).normalize();
        Vec3d waveCenter = player.getEntityPos().add(direction.multiply(RANGE / 2));
        
        Box box = new Box(waveCenter.subtract(RANGE / 2, 3, RANGE / 2), 
                         waveCenter.add(RANGE / 2, 3, RANGE / 2));
        
        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, box,
                e -> e != player && e.isAlive());

        for (LivingEntity entity : entities) {
            Vec3d knockbackDir = entity.getEntityPos().subtract(player.getEntityPos()).normalize();
            entity.setVelocity(entity.getVelocity().add(knockbackDir.multiply(KNOCKBACK).add(0, 0.5, 0)));
            entity.velocityDirty = true;
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1, false, false, true));
        }

        // Particle wave effect
        for (int i = 0; i < 30; i++) {
            double offset = (i - 15) * 0.4;
            Vec3d particlePos = player.getEntityPos().add(direction.multiply(i * 0.4))
                    .add(direction.rotateY((float) Math.PI / 2).multiply(offset * 0.3));
            world.spawnParticles(ParticleTypes.SPLASH, particlePos.x, particlePos.y + 0.5, particlePos.z, 
                    3, 0.3, 0.3, 0.3, 0.1);
            world.spawnParticles(ParticleTypes.BUBBLE, particlePos.x, particlePos.y + 0.5, particlePos.z, 
                    2, 0.3, 0.3, 0.3, 0.05);
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.PLAYERS, 1.5f, 0.8f);
        return true;
    }
}
