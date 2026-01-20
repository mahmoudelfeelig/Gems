package com.feel.gems.gametest.coverage;

import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.ModAbilities;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import java.util.EnumSet;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.Vec3d;

public final class GemsAbilityCoverageGameTests {
    private static void teleport(ServerPlayerEntity player, ServerWorld world, double x, double y, double z) {
        player.teleport(world, x, y, z, EnumSet.noneOf(PositionFlag.class), 0.0F, 0.0F, false);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void allAbilitiesActivateWithoutCrashing(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setEnergy(player, GemPlayerState.MAX_ENERGY);
        GemPowers.sync(player);
        player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));

        context.runAtTick(5L, () -> {
            for (Map.Entry<net.minecraft.util.Identifier, GemAbility> entry : ModAbilities.all().entrySet()) {
                try {
                    entry.getValue().activate(player);
                } catch (Throwable t) {
                    context.throwGameTestException("Ability crashed: " + entry.getKey() + " -> " + t.getMessage());
                    return;
                }
            }
            context.complete();
        });
    }
}
