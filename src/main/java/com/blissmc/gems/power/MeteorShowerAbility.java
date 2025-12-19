package com.blissmc.gems.power;

import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
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
        return 120 * 20;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        HitResult hit = player.raycast(60.0D, 1.0F, false);
        BlockPos center = BlockPos.ofFloored(hit.getPos());

        for (int i = 0; i < 10; i++) {
            double ox = (player.getRandom().nextDouble() - 0.5D) * 10.0D;
            double oz = (player.getRandom().nextDouble() - 0.5D) * 10.0D;
            Vec3d spawn = new Vec3d(center.getX() + 0.5D + ox, center.getY() + 25.0D + player.getRandom().nextDouble() * 10.0D, center.getZ() + 0.5D + oz);
            Vec3d dir = new Vec3d(-ox, -25.0D, -oz).normalize();

            FireballEntity meteor = new FireballEntity(world, player, dir, 1);
            meteor.refreshPositionAndAngles(spawn.x, spawn.y, spawn.z, 0.0F, 0.0F);
            meteor.setVelocity(dir.multiply(1.5D));
            world.spawnEntity(meteor);
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0F, 0.8F);
        return true;
    }
}

