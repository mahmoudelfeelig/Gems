package com.feel.gems.gametest.trade;

import com.feel.gems.core.GemId;
import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.item.ModItems;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.trade.GemTrading;
import java.util.EnumSet;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;




public final class GemsTradeGameTests {

    private static void teleport(ServerPlayerEntity player, ServerWorld world, double x, double y, double z, float yaw, float pitch) {
        player.teleport(world, x, y, z, EnumSet.noneOf(PositionFlag.class), yaw, pitch, false);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void gemTraderConsumesAndKeepsOnlyNewGem(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d startPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, startPos.x, startPos.y, startPos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.ASTRA);
        GemPlayerState.setOwnedGemsExact(player, EnumSet.of(GemId.ASTRA, GemId.FIRE));

        // Keep the main hand occupied so "ensurePlayerHasItem" doesn't accidentally place the new gem into the hand.
        player.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.STICK));
        player.setStackInHand(Hand.OFF_HAND, new ItemStack(ModItems.GEM_TRADER));
        player.giveItemStack(new ItemStack(ModItems.ASTRA_GEM));
        player.giveItemStack(new ItemStack(ModItems.FIRE_GEM));
        player.giveItemStack(new ItemStack(ModItems.LIFE_GEM));

        context.runAtTick(2L, () -> {
            int tradersBefore = GemsGameTestUtil.countItem(player, ModItems.GEM_TRADER);
            GemTrading.Result result = GemTrading.trade(player, GemId.FLUX);
            if (!result.success() || !result.consumedTrader()) {
                context.throwGameTestException("Trade did not succeed / did not consume gem_trader");
            }
            int tradersAfter = GemsGameTestUtil.countItem(player, ModItems.GEM_TRADER);
            if (tradersAfter != Math.max(0, tradersBefore - 1)) {
                context.throwGameTestException("Expected gem_trader count to decrement by 1, before=" + tradersBefore + " after=" + tradersAfter);
            }
            if (GemPlayerState.getActiveGem(player) != GemId.FLUX) {
                context.throwGameTestException("Active gem was not set to FLUX");
            }
            EnumSet<GemId> expectedOwned = EnumSet.of(GemId.FIRE, GemId.FLUX);
            if (!GemPlayerState.getOwnedGems(player).equals(expectedOwned)) {
                context.throwGameTestException("Owned gems mismatch, expected " + expectedOwned + " got " + GemPlayerState.getOwnedGems(player));
            }

            int gemItems = GemsGameTestUtil.countGemItems(player);
            boolean hasFlux = GemsGameTestUtil.hasItem(player, ModItems.FLUX_GEM);
            if (!hasFlux) {
                context.throwGameTestException("Player inventory did not contain the new FLUX gem item");
            }
            if (gemItems != 3) { // fire + life carried in + new flux
                context.throwGameTestException("Expected 3 gem items (kept others) after trading, found " + gemItems);
            }

            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void gemTraderFailsWithoutTrader(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d startPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, startPos.x, startPos.y, startPos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.ASTRA);
        GemPlayerState.setOwnedGemsExact(player, EnumSet.of(GemId.ASTRA));

        context.runAtTick(2L, () -> {
            GemTrading.Result result = GemTrading.trade(player, GemId.FLUX);
            if (result.success() || result.consumedTrader()) {
                context.throwGameTestException("Trade unexpectedly succeeded without a Trader");
            }
            if (GemPlayerState.getActiveGem(player) != GemId.ASTRA) {
                context.throwGameTestException("Active gem changed even though trade failed");
            }
            if (!GemPlayerState.getOwnedGems(player).equals(EnumSet.of(GemId.ASTRA))) {
                context.throwGameTestException("Owned gems changed even though trade failed");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void gemTraderRequiresItemAndConsumesExactlyOne(TestContext context) {
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        player.changeGameMode(GameMode.SURVIVAL);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.ASTRA);
        GemPlayerState.setOwnedGemsExact(player, java.util.EnumSet.of(GemId.ASTRA));

        // No gem_trader item present: should fail and not change state.
        GemTrading.Result fail = GemTrading.trade(player, GemId.FLUX);
        if (fail.success() || fail.consumedTrader()) {
            context.throwGameTestException("Trade should fail without a gem_trader item");
            return;
        }
        if (GemPlayerState.getActiveGem(player) != GemId.ASTRA) {
            context.throwGameTestException("Active gem should remain unchanged on failed trade");
            return;
        }

        // Fill part of the inventory, leave space for the gem.
        for (int i = 0; i < 5; i++) {
            player.getInventory().setStack(i, new ItemStack(Items.DIRT));
        }

        // Add exactly one gem_trader item and trade again.
        player.giveItemStack(new ItemStack(ModItems.GEM_TRADER));
        int tradersBefore = GemsGameTestUtil.countItem(player, ModItems.GEM_TRADER);

        GemTrading.Result ok = GemTrading.trade(player, GemId.FLUX);
        if (!ok.success() || !ok.consumedTrader()) {
            context.throwGameTestException("Trade should succeed and consume a gem_trader item");
            return;
        }
        int tradersAfter = GemsGameTestUtil.countItem(player, ModItems.GEM_TRADER);
        if (tradersAfter != Math.max(0, tradersBefore - 1)) {
            context.throwGameTestException("Gem_trader count did not decrement by 1");
            return;
        }

        if (GemPlayerState.getActiveGem(player) != GemId.FLUX) {
            context.throwGameTestException("Active gem should be Flux after trade");
            return;
        }
        if (!GemPlayerState.getOwnedGems(player).equals(java.util.EnumSet.of(GemId.FLUX))) {
            context.throwGameTestException("Owned gems should be reset to only Flux after trade");
            return;
        }

        int gemItems = GemsGameTestUtil.countGemItems(player);
        if (gemItems != 1 || !GemsGameTestUtil.hasItem(player, ModItems.FLUX_GEM)) {
            context.throwGameTestException("Inventory should contain exactly one Flux gem item after trade");
            return;
        }

        context.complete();
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void purchaseConsumesTokenAndKeepsAllOwned(TestContext context) {
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        player.changeGameMode(GameMode.SURVIVAL);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.ASTRA);
        GemPlayerState.setOwnedGemsExact(player, EnumSet.of(GemId.ASTRA, GemId.FIRE));

        player.getInventory().clear();
        player.giveItemStack(new ItemStack(ModItems.GEM_PURCHASE));
        player.giveItemStack(new ItemStack(ModItems.ASTRA_GEM));
        player.giveItemStack(new ItemStack(ModItems.FIRE_GEM));

        context.runAtTick(2L, () -> {
            int tokensBefore = GemsGameTestUtil.countItem(player, ModItems.GEM_PURCHASE);
            GemTrading.PurchaseResult result = GemTrading.purchase(player, GemId.FLUX);
            if (!result.success() || !result.consumedToken()) {
                context.throwGameTestException("Purchase did not succeed / did not consume token");
            }
            int tokensAfter = GemsGameTestUtil.countItem(player, ModItems.GEM_PURCHASE);
            if (tokensAfter != Math.max(0, tokensBefore - 1)) {
                context.throwGameTestException("Expected gem purchase token count to decrement by 1");
            }
            if (GemPlayerState.getActiveGem(player) != GemId.FLUX) {
                context.throwGameTestException("Active gem was not set to FLUX");
            }
            EnumSet<GemId> expectedOwned = EnumSet.of(GemId.ASTRA, GemId.FIRE, GemId.FLUX);
            if (!GemPlayerState.getOwnedGems(player).equals(expectedOwned)) {
                context.throwGameTestException("Owned gems mismatch, expected " + expectedOwned + " got " + GemPlayerState.getOwnedGems(player));
            }

            int gemItems = GemsGameTestUtil.countGemItems(player);
            if (!GemsGameTestUtil.hasItem(player, ModItems.FLUX_GEM)) {
                context.throwGameTestException("Player inventory did not contain the new FLUX gem item");
            }
            if (gemItems != 3) {
                context.throwGameTestException("Expected 3 gem items after purchase, found " + gemItems);
            }

            context.complete();
        });
    }

}

