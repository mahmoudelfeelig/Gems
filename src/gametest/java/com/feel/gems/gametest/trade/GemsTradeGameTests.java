package com.feel.gems.gametest.trade;

import java.util.EnumSet;
import com.feel.gems.assassin.AssassinState;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemDefinition;
import com.feel.gems.core.GemEnergyState;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.item.GemKeepOnDeath;
import com.feel.gems.item.GemItemGlint;
import com.feel.gems.item.ModItems;
import com.feel.gems.power.AbilityDisables;
import com.feel.gems.power.AbilityRuntime;
import com.feel.gems.power.AirDashAbility;
import com.feel.gems.power.AirUpdraftZoneAbility;
import com.feel.gems.power.AirWindJumpAbility;
import com.feel.gems.power.BeaconAuraAbility;
import com.feel.gems.power.BeaconAuraRuntime;
import com.feel.gems.power.FluxBeamAbility;
import com.feel.gems.power.FluxCharge;
import com.feel.gems.power.GemPowers;
import com.feel.gems.power.GemAbilities;
import com.feel.gems.power.PanicRingAbility;
import com.feel.gems.power.PillagerDiscipline;
import com.feel.gems.power.PillagerFangsAbility;
import com.feel.gems.power.PillagerVolleyAbility;
import com.feel.gems.power.PillagerVolleyRuntime;
import com.feel.gems.power.PowerIds;
import com.feel.gems.power.SpyMimicFormAbility;
import com.feel.gems.power.SpyMimicSystem;
import com.feel.gems.power.SpyStealAbility;
import com.feel.gems.power.SummonRecallAbility;
import com.feel.gems.power.SummonSlotAbility;
import com.feel.gems.power.SummonerSummons;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.trade.GemTrading;
import com.feel.gems.trust.GemTrust;
import com.feel.gems.util.GemsTime;
import com.feel.gems.power.GemAbilityCooldowns;
import com.feel.gems.net.CooldownSnapshotPayload;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.EvokerFangsEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryByteBuf;

public final class GemsTradeGameTests {
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void traderConsumesAndKeepsOnlyNewGem(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d startPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        player.teleport(world, startPos.x, startPos.y, startPos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.ASTRA);
        GemPlayerState.setOwnedGemsExact(player, EnumSet.of(GemId.ASTRA, GemId.FIRE));

        // Keep the main hand occupied so "ensurePlayerHasItem" doesn't accidentally place the new gem into the hand.
        player.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.STICK));
        player.setStackInHand(Hand.OFF_HAND, new ItemStack(ModItems.TRADER));
        player.giveItemStack(new ItemStack(ModItems.ASTRA_GEM));
        player.giveItemStack(new ItemStack(ModItems.FIRE_GEM));
        player.giveItemStack(new ItemStack(ModItems.LIFE_GEM));

        context.runAtTick(2L, () -> {
            int tradersBefore = GemsGameTestUtil.countItem(player, ModItems.TRADER);
            GemTrading.Result result = GemTrading.trade(player, GemId.FLUX);
            if (!result.success() || !result.consumedTrader()) {
                context.throwGameTestException("Trade did not succeed / did not consume trader");
            }
            int tradersAfter = GemsGameTestUtil.countItem(player, ModItems.TRADER);
            if (tradersAfter != Math.max(0, tradersBefore - 1)) {
                context.throwGameTestException("Expected trader count to decrement by 1, before=" + tradersBefore + " after=" + tradersAfter);
            }
            if (GemPlayerState.getActiveGem(player) != GemId.FLUX) {
                context.throwGameTestException("Active gem was not set to FLUX");
            }
            EnumSet<GemId> expectedOwned = EnumSet.of(GemId.FIRE, GemId.FLUX);
            if (!GemPlayerState.getOwnedGems(player).equals(expectedOwned)) {
                context.throwGameTestException("Owned gems mismatch, expected " + expectedOwned + " got " + GemPlayerState.getOwnedGems(player));
            }

            int gemItems = 0;
            boolean hasFlux = false;
            gemItems += GemsGameTestUtil.countGemItems(player.getInventory().main);
            hasFlux |= GemsGameTestUtil.hasItem(player, ModItems.FLUX_GEM);
            gemItems += GemsGameTestUtil.countGemItems(player.getInventory().offHand);
            gemItems += GemsGameTestUtil.countGemItems(player.getInventory().armor);
            if (!hasFlux) {
                context.throwGameTestException("Player inventory did not contain the new FLUX gem item");
            }
            if (gemItems != 3) { // fire + life carried in + new flux
                context.throwGameTestException("Expected 3 gem items (kept others) after trading, found " + gemItems);
            }

            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void traderFailsWithoutTrader(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d startPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        player.teleport(world, startPos.x, startPos.y, startPos.z, 0.0F, 0.0F);

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

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void traderRequiresItemAndConsumesExactlyOne(TestContext context) {
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.ASTRA);
        GemPlayerState.setOwnedGemsExact(player, java.util.EnumSet.of(GemId.ASTRA));

        // No trader item present: should fail and not change state.
        GemTrading.Result fail = GemTrading.trade(player, GemId.FLUX);
        if (fail.success() || fail.consumedTrader()) {
            context.throwGameTestException("Trade should fail without a trader item");
            return;
        }
        if (GemPlayerState.getActiveGem(player) != GemId.ASTRA) {
            context.throwGameTestException("Active gem should remain unchanged on failed trade");
            return;
        }

        // Fill part of the inventory, leave space for the gem.
        for (int i = 0; i < 5; i++) {
            player.getInventory().main.set(i, new ItemStack(Items.DIRT));
        }

        // Add exactly one trader item and trade again.
        player.giveItemStack(new ItemStack(ModItems.TRADER));
        int tradersBefore = GemsGameTestUtil.countItem(player, ModItems.TRADER);

        GemTrading.Result ok = GemTrading.trade(player, GemId.FLUX);
        if (!ok.success() || !ok.consumedTrader()) {
            context.throwGameTestException("Trade should succeed and consume a trader item");
            return;
        }
        int tradersAfter = GemsGameTestUtil.countItem(player, ModItems.TRADER);
        if (tradersAfter != Math.max(0, tradersBefore - 1)) {
            context.throwGameTestException("Trader count did not decrement by 1");
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

        int gemItems = GemsGameTestUtil.countGemItems(player.getInventory().main)
                + GemsGameTestUtil.countGemItems(player.getInventory().offHand)
                + GemsGameTestUtil.countGemItems(player.getInventory().armor);
        if (gemItems != 1 || !GemsGameTestUtil.hasItem(player, ModItems.FLUX_GEM)) {
            context.throwGameTestException("Inventory should contain exactly one Flux gem item after trade");
            return;
        }

        context.complete();
    }

}

