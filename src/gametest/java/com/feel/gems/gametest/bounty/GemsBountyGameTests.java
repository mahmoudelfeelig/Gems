package com.feel.gems.gametest.bounty;

import com.feel.gems.bounty.BountyBoard;
import com.feel.gems.bounty.BountyState;
import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.item.ModItems;
import com.feel.gems.state.GemPlayerState;
import java.util.EnumSet;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.packet.s2c.play.PositionFlag;

public final class GemsBountyGameTests {
    private static void teleport(ServerPlayerEntity player, ServerWorld world, double x, double y, double z, float yaw, float pitch) {
        player.teleport(world, x, y, z, EnumSet.noneOf(PositionFlag.class), yaw, pitch, false);
    }

    private static int countHearts(ServerPlayerEntity player) {
        int total = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(ModItems.HEART)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bountyPlaceAndClaimRewards(TestContext context) {
        ServerWorld world = context.getWorld();
        GemsGameTestUtil.placeStoneFloor(context, 6);

        ServerPlayerEntity placer = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity target = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity killer = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(placer);
        GemsGameTestUtil.forceSurvival(target);
        GemsGameTestUtil.forceSurvival(killer);

        Vec3d base = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(placer, world, base.x, base.y, base.z, 0.0F, 0.0F);
        teleport(target, world, base.x + 2.0D, base.y, base.z, 0.0F, 0.0F);
        teleport(killer, world, base.x - 2.0D, base.y, base.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(placer);
        GemPlayerState.setMaxHearts(placer, 8);
        GemPlayerState.applyMaxHearts(placer);
        GemPlayerState.setEnergy(placer, 4);
        GemPlayerState.initIfNeeded(killer);
        GemPlayerState.setEnergy(killer, 0);

        BountyBoard.PlaceResult result = BountyBoard.placeBounty(placer, target, 3, 2);
        if (result != BountyBoard.PlaceResult.SUCCESS) {
            context.throwGameTestException("Failed to place bounty");
            return;
        }
        if (GemPlayerState.getMaxHearts(placer) != 5 || GemPlayerState.getEnergy(placer) != 2) {
            context.throwGameTestException("Bounty cost did not deduct correctly");
            return;
        }

        context.runAtTick(5L, () -> BountyBoard.handleKill(target, killer, 0, 0));

        GemsGameTestUtil.assertEventually(
                context,
                6L,
                40L,
                2L,
                () -> countHearts(killer) >= 3
                        && GemPlayerState.getEnergy(killer) == 2
                        && BountyState.get(world.getServer()).getTarget(target.getUuid()) == null,
                "Bounty rewards were not delivered or cleared"
        );
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bountyTargetKillsPlacerPaysOut(TestContext context) {
        ServerWorld world = context.getWorld();
        GemsGameTestUtil.placeStoneFloor(context, 6);

        ServerPlayerEntity placer = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity target = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(placer);
        GemsGameTestUtil.forceSurvival(target);

        Vec3d base = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(placer, world, base.x, base.y, base.z, 0.0F, 0.0F);
        teleport(target, world, base.x + 2.0D, base.y, base.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(placer);
        GemPlayerState.setMaxHearts(placer, 7);
        GemPlayerState.applyMaxHearts(placer);
        GemPlayerState.setEnergy(placer, 3);
        GemPlayerState.initIfNeeded(target);
        GemPlayerState.setEnergy(target, 0);

        BountyBoard.PlaceResult result = BountyBoard.placeBounty(placer, target, 2, 1);
        if (result != BountyBoard.PlaceResult.SUCCESS) {
            context.throwGameTestException("Failed to place bounty");
            return;
        }

        context.runAtTick(5L, () -> BountyBoard.handleKill(placer, target, 0, 0));

        GemsGameTestUtil.assertEventually(
                context,
                6L,
                40L,
                2L,
                () -> countHearts(target) >= 2
                        && GemPlayerState.getEnergy(target) == 1
                        && BountyState.get(world.getServer()).getTarget(target.getUuid()) == null,
                "Target did not receive placer bounty"
        );
    }
}
