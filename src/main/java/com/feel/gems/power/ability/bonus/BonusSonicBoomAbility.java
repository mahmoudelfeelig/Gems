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
 * Sonic Boom - Emit a shockwave that deafens and staggers enemies.
 */
public final class BonusSonicBoomAbility implements GemAbility {
    private static final double RANGE = 10.0;
    private static final float DAMAGE = 6.0f;

    @Override
    public Identifier id() {
        return PowerIds.BONUS_SONIC_BOOM;
    }

    @Override
    public String name() {
        return "Sonic Boom";
    }

    @Override
    public String description() {
        return "Emit a shockwave that damages, staggers, and deafens enemies.";
    }

    @Override
    public int cooldownTicks() {
        return 300; // 15 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) return false;
        Box box = player.getBoundingBox().expand(RANGE);
        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, box,
                e -> e != player && e.isAlive());

        for (LivingEntity entity : entities) {
            // Damage
            entity.damage(world, world.getDamageSources().sonicBoom(player), DAMAGE);
            
            // Knockback away from player
            Vec3d knockback = entity.getEntityPos().subtract(player.getEntityPos()).normalize().multiply(1.5);
            entity.setVelocity(entity.getVelocity().add(knockback.x, 0.3, knockback.z));
            entity.velocityDirty = true;

            // Stagger effects
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 2, false, false, true));
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 100, 0, false, false, true));
        }

        // Expanding ring particle effect
        for (int ring = 1; ring <= 5; ring++) {
            double radius = ring * 2;
            for (int i = 0; i < 16; i++) {
                double angle = (2 * Math.PI / 16) * i;
                double x = player.getX() + Math.cos(angle) * radius;
                double z = player.getZ() + Math.sin(angle) * radius;
                world.spawnParticles(ParticleTypes.SONIC_BOOM, x, player.getY() + 1, z, 1, 0, 0, 0, 0);
            }
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 1.5f, 1.2f);
        return true;
    }
}
