package com.feel.gems.gametest.air;

import com.feel.gems.core.GemId;
import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.power.gem.air.AirMacePassive;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.TestContext;
import net.minecraft.world.GameMode;




public final class GemsAirMaceGameTests {
    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void airMaceGrantedOnceEver(TestContext context) {
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        player.changeGameMode(GameMode.SURVIVAL);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.AIR);
        GemPlayerState.setEnergy(player, 4);

        context.runAtTick(2L, () -> {
            GemPowers.sync(player);
            GemPowers.maintain(player);
            if (!GemsGameTestUtil.containsAirMace(player)) {
                context.throwGameTestException("Air mace should be granted when passive becomes active");
                return;
            }

            // Drop the mace - should NOT get another
            player.getInventory().clear();
            GemPowers.maintain(player);
            if (GemsGameTestUtil.containsAirMace(player)) {
                context.throwGameTestException("Air mace should never respawn after being dropped");
                return;
            }

            // Toggle energy off and on - still no mace
            GemPlayerState.setEnergy(player, 0);
            GemPowers.sync(player);
            GemPlayerState.setEnergy(player, 4);
            GemPowers.sync(player);
            GemPowers.maintain(player);

            if (GemsGameTestUtil.containsAirMace(player)) {
                context.throwGameTestException("Air mace should never respawn after energy toggle");
                return;
            }

            // Switch gems and back - still no mace
            GemPlayerState.setActiveGem(player, GemId.FIRE);
            GemPowers.sync(player);
            GemPlayerState.setActiveGem(player, GemId.AIR);
            GemPowers.sync(player);
            GemPowers.maintain(player);

            if (GemsGameTestUtil.containsAirMace(player)) {
                context.throwGameTestException("Air mace should never respawn after gem switch");
                return;
            }

            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void airMaceClearEverGrantedAllowsNewMace(TestContext context) {
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        player.changeGameMode(GameMode.SURVIVAL);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.AIR);
        GemPlayerState.setEnergy(player, 4);

        context.runAtTick(2L, () -> {
            GemPowers.sync(player);
            GemPowers.maintain(player);
            if (!GemsGameTestUtil.containsAirMace(player)) {
                context.throwGameTestException("Air mace should be granted when passive becomes active");
                return;
            }

            // Clear the mace and reset via admin command
            player.getInventory().clear();
            AirMacePassive.clearEverGranted(player);
            GemPowers.maintain(player);

            if (!GemsGameTestUtil.containsAirMace(player)) {
                context.throwGameTestException("Air mace should be granted after admin clearEverGranted");
                return;
            }

            context.complete();
        });
    }
}
