package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import com.feel.gems.util.GemsTeleport;

/**
 * Inferno Dash - Dash forward leaving a trail of fire.
 */
public final class BonusInfernoDashAbility implements GemAbility {
    private static final double DASH_DISTANCE = 10.0;

    @Override
    public Identifier id() {
        return PowerIds.BONUS_INFERNO_DASH;
    }

    @Override
    public String name() {
        return "Inferno Dash";
    }

    @Override
    public String description() {
        return "Dash forward 10 blocks, leaving a trail of fire behind.";
    }

    @Override
    public int cooldownTicks() {
        return 200; // 10 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) return false;
        Vec3d start = player.getEntityPos();
        Vec3d direction = player.getRotationVector().multiply(1, 0, 1).normalize();
        Vec3d end = start.add(direction.multiply(DASH_DISTANCE));

        // Teleport player
        GemsTeleport.teleport(player, world, end.x, end.y, end.z, player.getYaw(), player.getPitch());

        // Create fire trail
        Vec3d current = start;
        for (int i = 0; i < DASH_DISTANCE; i++) {
            BlockPos pos = BlockPos.ofFloored(current);
            BlockPos below = pos.down();
            if (world.getBlockState(pos).isAir() && world.getBlockState(below).isSolidBlock(world, below)) {
                world.setBlockState(pos, Blocks.FIRE.getDefaultState());
            }
            world.spawnParticles(ParticleTypes.FLAME, current.x, current.y + 0.5, current.z, 5, 0.2, 0.2, 0.2, 0.01);
            current = current.add(direction);
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        return true;
    }
}
