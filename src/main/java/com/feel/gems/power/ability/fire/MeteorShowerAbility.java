package com.feel.gems.power.ability.fire;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.util.RangeLimitedProjectile;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;



public final class MeteorShowerAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.METEOR_SHOWER;
    }

    @Override
    public String name() {
        return "Meteor Shower";
    }

    @Override
    public String description() {
        return "Calls multiple meteors that explode on impact around a target zone.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().fire().meteorShowerCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        HitResult hit = player.raycast(60.0D, 1.0F, false);
        BlockPos center = BlockPos.ofFloored(hit.getPos());
        AbilityFeedback.ring(world, new Vec3d(center.getX() + 0.5D, center.getY() + 0.2D, center.getZ() + 0.5D), 3.0D, ParticleTypes.FLAME, 24);

        int count = GemsBalance.v().fire().meteorShowerCount();
        int spread = GemsBalance.v().fire().meteorShowerSpreadBlocks();
        int height = GemsBalance.v().fire().meteorShowerHeightBlocks();
        float velocity = GemsBalance.v().fire().meteorShowerVelocity();
        int explosionPower = GemsBalance.v().fire().meteorShowerExplosionPower();
        double maxOffset = Math.max(0.0D, spread / 2.0D);

        for (int i = 0; i < count; i++) {
            double a = player.getRandom().nextDouble() * Math.PI * 2.0D;
            double r = Math.sqrt(player.getRandom().nextDouble()) * maxOffset;
            double ox = Math.cos(a) * r;
            double oz = Math.sin(a) * r;

            double impactX = center.getX() + 0.5D + ox;
            double impactZ = center.getZ() + 0.5D + oz;
            Vec3d spawn = new Vec3d(impactX, center.getY() + height, impactZ);
            Vec3d dir = new Vec3d(0.0D, -1.0D, 0.0D);

            // Use zero acceleration (for consistency) and set explicit velocity.
            FireballEntity meteor = new FireballEntity(world, player, Vec3d.ZERO, explosionPower);
            meteor.refreshPositionAndAngles(spawn.x, spawn.y, spawn.z, 0.0F, 0.0F);
            meteor.setVelocity(dir.multiply(Math.max(0.1D, velocity)));
            meteor.addCommandTag("gems_meteor");
            if (meteor instanceof RangeLimitedProjectile limited) {
                limited.gems$setRangeLimit(spawn, height + 64.0D);
            }
            world.spawnEntity(meteor);
        }

        AbilityFeedback.sound(player, SoundEvents.ENTITY_BLAZE_SHOOT, 1.0F, 0.8F);
        return true;
    }
}
