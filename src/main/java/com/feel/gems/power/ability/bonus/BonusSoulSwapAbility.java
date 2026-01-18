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

import com.feel.gems.util.GemsTeleport;

/**
 * Soul Swap - Swap positions with target player/mob.
 */
public final class BonusSoulSwapAbility implements GemAbility {
    private static final double RANGE = 25.0;

    @Override
    public Identifier id() {
        return PowerIds.BONUS_SOUL_SWAP;
    }

    @Override
    public String name() {
        return "Soul Swap";
    }

    @Override
    public String description() {
        return "Swap positions with a target within 25 blocks.";
    }

    @Override
    public int cooldownTicks() {
        return 400; // 20 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) return false;
        Vec3d start = player.getEyePos();
        Vec3d direction = player.getRotationVector();

        LivingEntity target = null;
        double bestDot = 0.75;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class,
                player.getBoundingBox().expand(RANGE), e -> e != player && e.isAlive())) {
            Vec3d toEntity = entity.getEyePos().subtract(start);
            double dist = toEntity.length();
            if (dist > RANGE || dist <= 0.0D) {
                continue;
            }
            double dot = toEntity.normalize().dotProduct(direction);
            if (dot < bestDot) {
                continue;
            }
            if (dot > bestDot || dist < bestDist) {
                bestDot = dot;
                bestDist = dist;
                target = entity;
            }
        }

        if (target == null) {
            return false;
        }

        Vec3d playerPos = player.getEntityPos();
        Vec3d targetPos = target.getEntityPos();
        float playerYaw = player.getYaw();
        float targetYaw = target.getYaw();

        // Particles at both locations before swap
        world.spawnParticles(ParticleTypes.SOUL, playerPos.x, playerPos.y + 1, playerPos.z, 
                20, 0.3, 0.5, 0.3, 0.05);
        world.spawnParticles(ParticleTypes.SOUL, targetPos.x, targetPos.y + 1, targetPos.z, 
                20, 0.3, 0.5, 0.3, 0.05);

        // Swap positions
        GemsTeleport.teleport(player, world, targetPos.x, targetPos.y, targetPos.z, playerYaw, player.getPitch());
        target.teleport(world, playerPos.x, playerPos.y, playerPos.z, Set.of(), targetYaw, target.getPitch(), false);

        // Particles after swap
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1, player.getZ(), 
                15, 0.3, 0.5, 0.3, 0.05);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, target.getX(), target.getY() + 1, target.getZ(), 
                15, 0.3, 0.5, 0.3, 0.05);

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        return true;
    }
}
