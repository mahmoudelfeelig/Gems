package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Set;

/**
 * Banishment - Teleport enemy far away randomly.
 */
public final class BonusBanishmentAbility implements GemAbility {
    private static final double RANGE = 20.0;
    private static final double BANISH_DISTANCE = 50.0;

    @Override
    public Identifier id() {
        return PowerIds.BONUS_BANISHMENT;
    }

    @Override
    public String name() {
        return "Banishment";
    }

    @Override
    public String description() {
        return "Teleport target enemy 50 blocks away in a random direction.";
    }

    @Override
    public int cooldownTicks() {
        return 600; // 30 seconds
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

        // Random direction teleport
        double angle = world.random.nextDouble() * Math.PI * 2;
        double offsetX = Math.cos(angle) * BANISH_DISTANCE;
        double offsetZ = Math.sin(angle) * BANISH_DISTANCE;
        
        Vec3d banishPos = target.getEntityPos().add(offsetX, 0, offsetZ);

        // Particles at original location
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, target.getX(), target.getY() + 1, target.getZ(), 
                30, 0.3, 0.5, 0.3, 0.1);

        // Teleport
        target.teleport(world, banishPos.x, banishPos.y, banishPos.z, Set.of(), target.getYaw(), target.getPitch(), false);

        // Particles at new location
        world.spawnParticles(ParticleTypes.PORTAL, banishPos.x, banishPos.y + 1, banishPos.z, 
                30, 0.3, 0.5, 0.3, 0.1);

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.5f, 0.5f);
        return true;
    }
}
