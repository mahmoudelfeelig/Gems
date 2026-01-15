package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

public final class BonusMagmaPoolAbility implements GemAbility {
    private static final int RANGE_BLOCKS = 16;
    private static final int RADIUS_BLOCKS = 2;

    @Override
    public Identifier id() {
        return PowerIds.BONUS_MAGMA_POOL;
    }

    @Override
    public String name() {
        return "Magma Pool";
    }

    @Override
    public String description() {
        return "Create a pool of lava at the target location.";
    }

    @Override
    public int cooldownTicks() {
        return 800; // 40 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        HitResult hit = player.raycast(RANGE_BLOCKS, 1.0F, false);
        BlockPos ground = hit instanceof BlockHitResult bhr ? bhr.getBlockPos() : player.getBlockPos().down();
        BlockPos center = ground.up();

        for (int dx = -RADIUS_BLOCKS; dx <= RADIUS_BLOCKS; dx++) {
            for (int dz = -RADIUS_BLOCKS; dz <= RADIUS_BLOCKS; dz++) {
                if ((dx * dx + dz * dz) > (RADIUS_BLOCKS * RADIUS_BLOCKS)) {
                    continue;
                }
                BlockPos pos = center.add(dx, 0, dz);
                if (world.getBlockState(pos).isReplaceable() || world.getBlockState(pos).isAir()) {
                    world.setBlockState(pos, Blocks.LAVA.getDefaultState());
                }
            }
        }

        world.spawnParticles(ParticleTypes.FLAME, center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D, 50, 2, 0.5, 2, 0.1);
        return true;
    }
}
