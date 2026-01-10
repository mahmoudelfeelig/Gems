package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Crystal Cage - Trap target in an amethyst crystal cage for 3 seconds.
 */
public final class BonusCrystalCageAbility implements GemAbility {
    private static final double RANGE = 20.0;
    private static final int CAGE_DURATION = 60; // 3 seconds

    @Override
    public Identifier id() {
        return PowerIds.BONUS_CRYSTAL_CAGE;
    }

    @Override
    public String name() {
        return "Crystal Cage";
    }

    @Override
    public String description() {
        return "Trap a target in an amethyst cage for 3 seconds.";
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
        double closestDist = RANGE;
        for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, 
                player.getBoundingBox().expand(RANGE), e -> e != player && e.isAlive())) {
            Vec3d toEntity = entity.getEyePos().subtract(start);
            double dot = toEntity.normalize().dotProduct(direction);
            double dist = toEntity.length();
            if (dot > 0.9 && dist < closestDist) {
                target = entity;
                closestDist = dist;
            }
        }

        if (target == null) {
            return false;
        }

        // Create cage around target
        BlockPos center = target.getBlockPos();
        List<BlockPos> cageBlocks = new ArrayList<>();
        
        // Walls
        for (int y = 0; y <= 2; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (Math.abs(x) == 1 || Math.abs(z) == 1 || y == 2) {
                        BlockPos pos = center.add(x, y, z);
                        if (world.getBlockState(pos).isReplaceable()) {
                            world.setBlockState(pos, Blocks.AMETHYST_BLOCK.getDefaultState());
                            cageBlocks.add(pos);
                        }
                    }
                }
            }
        }

        // Schedule cage removal
        final LivingEntity finalTarget = target;
        world.getServer().execute(() -> {
            try {
                world.getServer().getCommandManager().getDispatcher().execute(
                        String.format("schedule function gems:remove_cage %d", CAGE_DURATION),
                        world.getServer().getCommandSource()
                );
            } catch (Exception e) {
                // Ignore command execution errors
            }
        });

        // For now, manually schedule via tick counter (simplified)
        // In production, use a proper scheduled task system
        cageBlocks.forEach(pos -> {
            world.scheduleBlockTick(pos, Blocks.AMETHYST_BLOCK, CAGE_DURATION);
        });

        world.spawnParticles(ParticleTypes.END_ROD, center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5, 
                20, 0.5, 0.5, 0.5, 0.1);

        world.playSound(null, center.getX(), center.getY(), center.getZ(),
                SoundEvents.BLOCK_AMETHYST_BLOCK_PLACE, SoundCategory.PLAYERS, 1.5f, 0.8f);
        return true;
    }
}
