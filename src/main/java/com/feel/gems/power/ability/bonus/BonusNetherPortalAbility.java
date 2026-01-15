package com.feel.gems.power.ability.bonus;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.util.GemsTeleport;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

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
        var hit = world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
        Vec3d dest = hit.getPos();
        BlockPos blockPos = BlockPos.ofFloored(dest);
        if (!world.getBlockState(blockPos).isAir()) {
            blockPos = blockPos.offset(Direction.UP);
        }
        GemsTeleport.teleport(player, world, blockPos.getX() + 0.5D, blockPos.getY() + 0.1D, blockPos.getZ() + 0.5D, player.getYaw(), player.getPitch());
        world.spawnParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.4, 0.6, 0.4, 0.02);
        return true;
    }
}
