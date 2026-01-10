package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class BonusIceWallAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_ICE_WALL;
    }

    @Override
    public String name() {
        return "Ice Wall";
    }

    @Override
    public String description() {
        return "Create a wall of ice where you're looking.";
    }

    @Override
    public int cooldownTicks() {
        return 600; // 30 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        Vec3d start = player.getEyePos();
        Vec3d direction = player.getRotationVector();
        Vec3d end = start.add(direction.multiply(10));
        
        HitResult hit = world.raycast(new RaycastContext(
                start, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player));
        
        BlockPos center = BlockPos.ofFloored(hit.getPos());
        Vec3d perpendicular = new Vec3d(-direction.z, 0, direction.x).normalize();
        
        for (int h = 0; h < 4; h++) {
            for (int w = -2; w <= 2; w++) {
                BlockPos pos = center.add((int)(perpendicular.x * w), h, (int)(perpendicular.z * w));
                if (world.getBlockState(pos).isReplaceable() || world.getBlockState(pos).isAir()) {
                    world.setBlockState(pos, Blocks.PACKED_ICE.getDefaultState());
                }
            }
        }
        
        world.spawnParticles(ParticleTypes.SNOWFLAKE, center.getX(), center.getY() + 2, center.getZ(), 40, 2, 2, 2, 0.1);
        return true;
    }
}
