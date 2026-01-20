package com.feel.gems.power.ability.bonus;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

/**
 * Nether Portal - short-range teleport through a rift.
 */
public final class BonusNetherPortalAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_NETHER_PORTAL;
    }

    @Override
    public String name() {
        return "Nether Portal";
    }

    @Override
    public String description() {
        return "Short-range teleport through a nether rift.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().bonusPool().netherPortalCooldownSeconds * 20;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        int range = GemsBalance.v().bonusPool().netherPortalDistanceBlocks;
        Vec3d start = player.getEyePos();
        Vec3d dir = player.getRotationVec(1.0F);
        Vec3d end = start.add(dir.multiply(range));
        BlockHitResult hit = world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) {
            return false;
        }
        BlockPos base = hit.getBlockPos().offset(hit.getSide());
        Direction.Axis axis = player.getHorizontalFacing().getAxis() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        boolean built = buildPortal(world, base, axis);
        if (!built) {
            return false;
        }
        world.spawnParticles(ParticleTypes.PORTAL, base.getX() + 0.5D, base.getY() + 1.0, base.getZ() + 0.5D, 24, 0.6, 0.8, 0.6, 0.02);
        return true;
    }

    private static boolean buildPortal(ServerWorld world, BlockPos base, Direction.Axis axis) {
        Direction right = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        BlockPos origin = base;
        int width = 4;
        int height = 5;
        if (!canFitPortal(world, origin, right, width, height)) {
            return false;
        }
        for (int y = 0; y < height; y++) {
            for (int w = 0; w < width; w++) {
                if (y != 0 && y != height - 1 && w != 0 && w != width - 1) {
                    continue;
                }
                BlockPos pos = origin.add(right.getOffsetX() * w, y, right.getOffsetZ() * w);
                world.setBlockState(pos, Blocks.OBSIDIAN.getDefaultState());
            }
        }
        BlockState portal = Blocks.NETHER_PORTAL.getDefaultState().with(NetherPortalBlock.AXIS, axis);
        for (int y = 1; y < height - 1; y++) {
            for (int w = 1; w < width - 1; w++) {
                BlockPos pos = origin.add(right.getOffsetX() * w, y, right.getOffsetZ() * w);
                world.setBlockState(pos, portal, 3);
            }
        }
        return true;
    }

    private static boolean canFitPortal(ServerWorld world, BlockPos origin, Direction right, int width, int height) {
        for (int y = 0; y < height; y++) {
            for (int w = 0; w < width; w++) {
                BlockPos pos = origin.add(right.getOffsetX() * w, y, right.getOffsetZ() * w);
                BlockState state = world.getBlockState(pos);
                if (!state.isAir() && !state.isReplaceable() && !state.isOf(Blocks.OBSIDIAN)) {
                    return false;
                }
            }
        }
        return true;
    }
}
