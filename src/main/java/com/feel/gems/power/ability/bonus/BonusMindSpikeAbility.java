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
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Mind Spike - Psychic damage that also reveals enemy location.
 */
public final class BonusMindSpikeAbility implements GemAbility {
    private static final double RANGE = 20.0;
    private static final float DAMAGE = 6.0f;
    private static final int GLOW_DURATION = 200; // 10 seconds

    @Override
    public Identifier id() {
        return PowerIds.BONUS_MIND_SPIKE;
    }

    @Override
    public String name() {
        return "Mind Spike";
    }

    @Override
    public String description() {
        return "Deal 6 psychic damage and reveal target's location for 10s.";
    }

    @Override
    public int cooldownTicks() {
        return 200; // 10 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) return false;
        Vec3d start = player.getEyePos();
        Vec3d direction = player.getRotationVector();

        LivingEntity target = null;
        for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, 
                player.getBoundingBox().expand(RANGE), e -> e != player && e.isAlive())) {
            Vec3d toEntity = entity.getEyePos().subtract(start);
            double dot = toEntity.normalize().dotProduct(direction);
            if (dot > 0.9 && toEntity.length() < RANGE) {
                target = entity;
                break;
            }
        }

        if (target == null) {
            return false;
        }

        // Psychic damage
        target.damage(world, player.getDamageSources().indirectMagic(player, player), DAMAGE);
        
        // Apply glowing to reveal location
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, GLOW_DURATION, 0, false, false, true));
        
        // Brief confusion
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 60, 0, false, false, true));

        // Visual effect - line from player to target
        Vec3d targetPos = target.getEyePos();
        Vec3d diff = targetPos.subtract(start);
        for (int i = 0; i < 20; i++) {
            Vec3d particlePos = start.add(diff.multiply(i / 20.0));
            world.spawnParticles(ParticleTypes.ENCHANT, particlePos.x, particlePos.y, particlePos.z, 
                    1, 0.05, 0.05, 0.05, 0.01);
        }

        world.spawnParticles(ParticleTypes.WITCH, target.getX(), target.getY() + target.getHeight(), target.getZ(), 
                15, 0.2, 0.2, 0.2, 0.05);

        world.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.ENTITY_PHANTOM_HURT, SoundCategory.PLAYERS, 1.0f, 2.0f);
        return true;
    }
}
