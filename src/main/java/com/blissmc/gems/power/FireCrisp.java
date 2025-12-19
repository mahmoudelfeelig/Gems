package com.blissmc.gems.power;

import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class FireCrisp {
    private FireCrisp() {
    }

    public static void apply(ServerWorld world, BlockPos center, int radius) {
        int r = Math.max(1, radius);
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState state = world.getBlockState(pos);
                    if (!state.getFluidState().isIn(FluidTags.WATER)) {
                        continue;
                    }
                    if (!world.getBlockState(pos).getFluidState().isIn(FluidTags.WATER)) {
                        continue;
                    }
                    world.breakBlock(pos, false);
                }
            }
        }
    }
}

