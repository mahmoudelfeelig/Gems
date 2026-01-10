package com.feel.gems.power.ability.sentinel;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.state.PlayerStateManager;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public final class SentinelShieldWallAbility implements GemAbility {
    public static final String WALL_ACTIVE_KEY = "sentinel_wall_active";
    public static final String WALL_END_KEY = "sentinel_wall_end";

    @Override
    public Identifier id() {
        return PowerIds.SENTINEL_SHIELD_WALL;
    }

    @Override
    public String name() {
        return "Shield Wall";
    }

    @Override
    public String description() {
        return "Deploy an energy barrier that blocks projectiles and slows enemies passing through.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().sentinel().shieldWallCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        int durationTicks = GemsBalance.v().sentinel().shieldWallDurationTicks();
        int width = GemsBalance.v().sentinel().shieldWallWidthBlocks();
        int height = GemsBalance.v().sentinel().shieldWallHeightBlocks();

        // Get facing direction
        Direction facing = player.getHorizontalFacing();
        Direction perpendicular = facing.rotateYClockwise();

        // Position wall in front of player
        BlockPos basePos = player.getBlockPos().offset(facing, 2);

        long endTime = world.getTime() + durationTicks;
        PlayerStateManager.setPersistent(player, WALL_ACTIVE_KEY, "true");
        PlayerStateManager.setPersistent(player, WALL_END_KEY, String.valueOf(endTime));

        // Store wall position data for runtime checks
        SentinelShieldWallRuntime.createWall(player, basePos, perpendicular, width, height, endTime);

        // Visual effect - particle wall
        for (int w = -width / 2; w <= width / 2; w++) {
            for (int h = 0; h < height; h++) {
                BlockPos wallPos = basePos.offset(perpendicular, w).up(h);
                AbilityFeedback.burstAt(world, Vec3d.ofCenter(wallPos), ParticleTypes.END_ROD, 5, 0.2D);
            }
        }

        AbilityFeedback.sound(player, SoundEvents.BLOCK_BEACON_ACTIVATE, 1.0F, 1.5F);
        return true;
    }
}
