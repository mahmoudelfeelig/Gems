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
 * Gravity Crush - Slam target to ground, rooting and damaging them.
 */
public final class BonusGravityCrushAbility implements GemAbility {
    private static final double RANGE = 20.0;
    private static final float DAMAGE = 10.0f;
    private static final int ROOT_DURATION = 60; // 3 seconds

    @Override
    public Identifier id() {
        return PowerIds.BONUS_GRAVITY_CRUSH;
    }

    @Override
    public String name() {
        return "Gravity Crush";
    }

    @Override
    public String description() {
        return "Slam a target to the ground, dealing 10 damage and rooting for 3s.";
    }

    @Override
    public int cooldownTicks() {
        return 350; // 17.5 seconds
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

        // Slam target down
        target.setVelocity(0, -2.0, 0);
        target.velocityDirty = true;

        // Deal damage
        target.damage(world, world.getDamageSources().magic(), DAMAGE);

        // Root effect
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, ROOT_DURATION, 127, false, false, true));
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, ROOT_DURATION, 128, false, false, false));

        // Ground impact particles
        for (int i = 0; i < 20; i++) {
            double angle = world.random.nextDouble() * Math.PI * 2;
            double radius = world.random.nextDouble() * 2;
            double x = target.getX() + Math.cos(angle) * radius;
            double z = target.getZ() + Math.sin(angle) * radius;
            world.spawnParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, x, target.getY() + 0.1, z, 
                    1, 0, 0.1, 0, 0.01);
        }

        world.spawnParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY(), target.getZ(), 
                1, 0, 0, 0, 0);

        world.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.PLAYERS, 1.0f, 0.5f);
        return true;
    }
}
