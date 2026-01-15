package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Vortex Strike - Spin attack that pulls enemies closer and damages them.
 */
public final class BonusVortexStrikeAbility implements GemAbility {
    private static final double RANGE = 6.0;
    private static final float DAMAGE = 6.0f;
    private static final double PULL_STRENGTH = 0.8;

    @Override
    public Identifier id() {
        return PowerIds.BONUS_VORTEX_STRIKE;
    }

    @Override
    public String name() {
        return "Vortex Strike";
    }

    @Override
    public String description() {
        return "Spin attack that pulls enemies within 6 blocks toward you.";
    }

    @Override
    public int cooldownTicks() {
        return 200; // 10 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) return false;
        Box box = player.getBoundingBox().expand(RANGE);
        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && !(e instanceof ServerPlayerEntity p && p.isCreative()));

        if (entities.isEmpty()) {
            return false;
        }

        for (LivingEntity entity : entities) {
            if (entity instanceof ServerPlayerEntity otherPlayer) {
                if (VoidImmunity.shouldBlockEffect(player, otherPlayer)) {
                    continue;
                }
                if (GemTrust.isTrusted(player, otherPlayer)) {
                    continue;
                }
            }
            // Deal damage (avoid vanilla melee knockback fighting the pull).
            entity.damage(world, world.getDamageSources().magic(), DAMAGE);

            // Pull toward player.
            Vec3d pullDir = player.getEntityPos().subtract(entity.getEntityPos()).normalize().multiply(PULL_STRENGTH);
            Vec3d vel = entity.getVelocity().add(pullDir.x, 0.1, pullDir.z);
            entity.setVelocity(vel);
            entity.velocityDirty = true;
        }

        // Spinning particle effect
        for (int ring = 0; ring < 3; ring++) {
            for (int i = 0; i < 16; i++) {
                double angle = (2 * Math.PI / 16) * i + ring * 0.5;
                double radius = 2 + ring;
                double x = player.getX() + Math.cos(angle) * radius;
                double z = player.getZ() + Math.sin(angle) * radius;
                world.spawnParticles(ParticleTypes.SWEEP_ATTACK, x, player.getY() + 0.5, z, 
                        1, 0, 0, 0, 0);
            }
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.5f, 0.8f);
        return true;
    }
}
