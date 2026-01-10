package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class BonusMagmaPoolAbility implements GemAbility {
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
        return "Create a pool of magma blocks beneath your feet.";
    }

    @Override
    public int cooldownTicks() {
        return 800; // 40 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        BlockPos center = player.getBlockPos().down();
        
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos pos = center.add(dx, 0, dz);
                if (world.getBlockState(pos).isReplaceable() || world.getBlockState(pos).isAir()) {
                    world.setBlockState(pos, Blocks.MAGMA_BLOCK.getDefaultState());
                }
            }
        }
        
        world.spawnParticles(ParticleTypes.FLAME, player.getX(), player.getY(), player.getZ(), 50, 2, 0.5, 2, 0.1);
        return true;
    }
}
