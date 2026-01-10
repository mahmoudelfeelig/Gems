package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.util.RangeLimitedProjectile;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.ExplosiveProjectileEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public final class BonusDoomBoltAbility implements GemAbility {
    private static final int COOLDOWN_TICKS = 800; // 40 seconds
    private static final float DAMAGE = 12.0F;
    private static final float VELOCITY = 0.5F;
    private static final double MAX_RANGE_BLOCKS = 30.0D;

    @Override
    public Identifier id() {
        return PowerIds.BONUS_DOOM_BOLT;
    }

    @Override
    public String name() {
        return "Doom Bolt";
    }

    @Override
    public String description() {
        return "Launch a slow but devastating dark projectile.";
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        Vec3d direction = player.getRotationVec(1.0F).normalize();
        Vec3d spawn = player.getEyePos().add(direction.multiply(0.6D));

        WitherSkullEntity bolt = EntityType.WITHER_SKULL.create(world, SpawnReason.MOB_SUMMONED);
        if (bolt == null) {
            return false;
        }
        bolt.setOwner(player);
        bolt.refreshPositionAndAngles(spawn.x, spawn.y, spawn.z, player.getYaw(), player.getPitch());
        bolt.setVelocity(direction.x, direction.y, direction.z, VELOCITY, 0.0F);
        bolt.addCommandTag("gems_doom_bolt");

        if (bolt instanceof ExplosiveProjectileEntity explosive) {
            if (explosive instanceof RangeLimitedProjectile limited) {
                limited.gems$setRangeLimit(spawn, MAX_RANGE_BLOCKS);
            }
        }

        world.spawnEntity(bolt);

        world.spawnParticles(ParticleTypes.SMOKE, spawn.x, spawn.y, spawn.z, 12, 0.3, 0.3, 0.3, 0.02);
        world.spawnParticles(ParticleTypes.SOUL, spawn.x, spawn.y, spawn.z, 6, 0.2, 0.2, 0.2, 0.01);
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sound.SoundEvents.ENTITY_WITHER_SHOOT, net.minecraft.sound.SoundCategory.PLAYERS, 0.8f, 0.6f);
        return true;
    }

    public static float doomBoltDamage() {
        return DAMAGE;
    }
}
