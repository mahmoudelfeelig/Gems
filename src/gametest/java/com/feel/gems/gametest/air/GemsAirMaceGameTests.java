package com.feel.gems.gametest.air;

import com.feel.gems.core.GemId;
import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.TestContext;
import net.minecraft.world.GameMode;




public final class GemsAirMaceGameTests {
    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void airMaceDoesNotRespawnWhenDropped(TestContext context) {
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

            // Simulate the player dropping/ditching the mace: it should NOT immediately respawn.
            player.getInventory().clear();
            GemPowers.maintain(player);
            if (GemsGameTestUtil.containsAirMace(player)) {
                context.throwGameTestException("Air mace should not respawn after being dropped while passive remains active");
                return;
            }

            // If the passive is disabled and re-enabled, a new mace can be granted again.
            GemPlayerState.setEnergy(player, 0);
            GemPowers.sync(player);
            GemPlayerState.setEnergy(player, 4);
            GemPowers.sync(player);
            GemPowers.maintain(player);

            if (!GemsGameTestUtil.containsAirMace(player)) {
                context.throwGameTestException("Air mace should be granted again after passive re-enabled");
                return;
            }

            context.complete();
        });
    }
}
