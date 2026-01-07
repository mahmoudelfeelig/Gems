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

import com.feel.gems.util.GemsTeleport;

/**
 * Warp Strike - Teleport behind target and strike.
 */
public final class BonusWarpStrikeAbility implements GemAbility {
    private static final double RANGE = 20.0;
    private static final float DAMAGE = 8.0f;

    @Override
    public Identifier id() {
        return PowerIds.BONUS_WARP_STRIKE;
    }

    @Override
    public String name() {
        return "Warp Strike";
    }

    @Override
    public String description() {
        return "Teleport behind a target and deal 8 damage.";
    }

    @Override
    public int cooldownTicks() {
        return 300; // 15 seconds
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

        // Calculate position behind target
        Vec3d targetFacing = Vec3d.fromPolar(0, target.getYaw()).normalize();
        Vec3d behindPos = target.getEntityPos().subtract(targetFacing.multiply(2));

        // Particles at start
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1, player.getZ(), 
                15, 0.3, 0.5, 0.3, 0.05);

        // Teleport behind target
        GemsTeleport.teleport(player, world, behindPos.x, behindPos.y, behindPos.z, 
                target.getYaw() + 180, player.getPitch());

        // Deal damage
        target.damage(world, world.getDamageSources().playerAttack(player), DAMAGE);

        // Particles at end
        world.spawnParticles(ParticleTypes.SWEEP_ATTACK, player.getX() + targetFacing.x, 
                player.getY() + 1, player.getZ() + targetFacing.z, 1, 0, 0, 0, 0);

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0f, 1.0f);
        return true;
    }
}
