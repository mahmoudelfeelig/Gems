package com.feel.gems.gametest.space;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.power.gem.space.SpaceAnomalies;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;




public final class GemsSpaceMiningGameTests {
    private static void teleport(ServerPlayerEntity player, ServerWorld world, double x, double y, double z, float yaw, float pitch) {
        player.teleport(world, x, y, z, java.util.EnumSet.noneOf(PositionFlag.class), yaw, pitch, false);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void orbitalLaserMiningBreaksNormalBlocks(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d startPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, startPos.x, startPos.y, startPos.z, 0.0F, 0.0F);

        BlockPos target = context.getAbsolutePos(new BlockPos(0, 2, 3));
        world.setBlockState(target, Blocks.STONE.getDefaultState());

        int delay = GemsBalance.v().space().orbitalLaserDelayTicks();
        context.runAtTick(2L, () -> SpaceAnomalies.scheduleOrbitalLaser(player, target, true));

        context.runAtTick(2L + delay + 5L, () -> {
            if (!world.getBlockState(target).isAir()) {
                context.throwGameTestException("Orbital Laser (mining) did not break the target block");
                return;
            }
            context.complete();
        });
    }
}
