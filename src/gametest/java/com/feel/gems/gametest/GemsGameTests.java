package com.feel.gems.gametest;

import java.util.EnumSet;

import com.feel.gems.core.GemId;
import com.feel.gems.item.GemKeepOnDeath;
import com.feel.gems.item.ModItems;
import com.feel.gems.power.AbilityRuntime;
import com.feel.gems.power.FluxBeamAbility;
import com.feel.gems.power.FluxCharge;
import com.feel.gems.power.GemPowers;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.trade.GemTrading;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

public final class GemsGameTests {
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void astralCameraReturnsToStart(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d startPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        BlockPos startBlock = BlockPos.ofFloored(startPos);
        player.teleport(world, startPos.x, startPos.y, startPos.z, 0.0F, 0.0F);

        AbilityRuntime.startAstralCamera(player, 20);

        Vec3d movedPos = context.getAbsolute(new Vec3d(6.5D, 2.0D, 0.5D));
        player.teleport(world, movedPos.x, movedPos.y, movedPos.z, 90.0F, 0.0F);

        context.runAtTick(80L, () -> {
            if (!player.getBlockPos().equals(startBlock)) {
                context.throwGameTestException("Astral Camera did not return player to start position");
            }
            if (player.interactionManager.getGameMode() != GameMode.SURVIVAL) {
                context.throwGameTestException("Astral Camera did not restore original gamemode");
            }
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void fluxBeamConsumesCharge(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d startPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        player.teleport(world, startPos.x, startPos.y, startPos.z, 0.0F, 0.0F);

        // Place a target in front of the player and explicitly aim at it (GameTest runs all tests in one batch,
        // so relying on default rotations or relative spawn helpers can be flaky).
        var target = EntityType.ARMOR_STAND.create(world);
        if (target == null) {
            context.throwGameTestException("Failed to create target entity");
            return;
        }
        // Keep the target very close so it stays within the default EMPTY_STRUCTURE bounds (avoids barrier occlusion).
        Vec3d targetPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 1.5D));
        target.refreshPositionAndAngles(targetPos.x, targetPos.y, targetPos.z, 0.0F, 0.0F);
        target.setNoGravity(true);
        world.spawnEntity(target);

        Vec3d aimAt = new Vec3d(target.getX(), target.getEyeY(), target.getZ());
        double dx = aimAt.x - player.getX();
        double dz = aimAt.z - player.getZ();
        double dy = aimAt.y - player.getEyeY();
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
        // teleport(...) is the most reliable way to apply yaw/pitch for server-side raycasts.
        player.teleport(world, startPos.x, startPos.y, startPos.z, yaw, pitch);

        FluxCharge.set(player, 100);

        context.runAtTick(5L, () -> {
            boolean activated = new FluxBeamAbility().activate(player);
            if (!activated) {
                context.throwGameTestException("Flux Beam did not activate (target not acquired)");
            }
            if (FluxCharge.get(player) != 0) {
                context.throwGameTestException("Flux Beam did not reset flux charge back to 0");
            }
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void deathKeepsActiveGemOnly(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d startPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        player.teleport(world, startPos.x, startPos.y, startPos.z, 0.0F, 0.0F);

        // GameTest creates "embedded" players and runs normal join callbacks; to avoid racey interactions with
        // JOIN/COPY_FROM logic (e.g. ensureActiveGemItem), perform test setup after the player is fully connected.
        context.runAtTick(20L, () -> {
            player.getInventory().clear();
            GemPlayerState.initIfNeeded(player);
            GemPlayerState.setActiveGem(player, GemId.ASTRA);
            player.giveItemStack(new ItemStack(ModItems.ASTRA_GEM));
            player.giveItemStack(new ItemStack(ModItems.FIRE_GEM));
        });

        // GameTest servers often run with gamerules that can affect drops (e.g. keepInventory).
        // Validate the core stash/restore behavior directly:
        // - stash removes exactly one active gem item (kept across death)
        // - stash does not remove non-active gems
        // - restore returns the active gem item
        context.runAtTick(40L, () -> {
            if (!hasItem(player, ModItems.ASTRA_GEM)) {
                context.throwGameTestException("Setup error: missing active Astra gem item");
            }
            if (!hasItem(player, ModItems.FIRE_GEM)) {
                context.throwGameTestException("Setup error: missing non-active Fire gem item");
            }

            int astraBefore = countItem(player, ModItems.ASTRA_GEM);
            int fireBefore = countItem(player, ModItems.FIRE_GEM);

            GemKeepOnDeath.stash(player);

            int astraAfterStash = countItem(player, ModItems.ASTRA_GEM);
            int fireAfterStash = countItem(player, ModItems.FIRE_GEM);

            if (astraAfterStash != Math.max(0, astraBefore - 1)) {
                context.throwGameTestException("Active gem item should decrement by 1 after stash, before=" + astraBefore + " after=" + astraAfterStash);
            }
            if (fireAfterStash != fireBefore) {
                context.throwGameTestException("Non-active gem item should not change after stash, before=" + fireBefore + " after=" + fireAfterStash);
            }

            GemKeepOnDeath.restore(player);
            int astraAfterRestore = countItem(player, ModItems.ASTRA_GEM);
            if (astraAfterRestore != astraBefore) {
                context.throwGameTestException("Active gem item should be restored after stash+restore, before=" + astraBefore + " after=" + astraAfterRestore);
            }

            context.complete();
        });
    }

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
            int tradersBefore = countItem(player, ModItems.TRADER);
            GemTrading.Result result = GemTrading.trade(player, GemId.FLUX);
            if (!result.success() || !result.consumedTrader()) {
                context.throwGameTestException("Trade did not succeed / did not consume trader");
            }
            int tradersAfter = countItem(player, ModItems.TRADER);
            if (tradersAfter != Math.max(0, tradersBefore - 1)) {
                context.throwGameTestException("Expected trader count to decrement by 1, before=" + tradersBefore + " after=" + tradersAfter);
            }
            if (GemPlayerState.getActiveGem(player) != GemId.FLUX) {
                context.throwGameTestException("Active gem was not set to FLUX");
            }
            if (!GemPlayerState.getOwnedGems(player).equals(EnumSet.of(GemId.FLUX))) {
                context.throwGameTestException("Owned gems were not reset to only FLUX");
            }

            int gemItems = 0;
            boolean hasFlux = false;
            gemItems += countGemItems(player.getInventory().main);
            hasFlux |= hasItem(player, ModItems.FLUX_GEM);
            gemItems += countGemItems(player.getInventory().offHand);
            gemItems += countGemItems(player.getInventory().armor);
            if (!hasFlux) {
                context.throwGameTestException("Player inventory did not contain the new FLUX gem item");
            }
            if (gemItems != 1) {
                context.throwGameTestException("Expected exactly 1 gem item after trading, found " + gemItems);
            }

            context.complete();
        });
    }

    private static boolean hasItem(ServerPlayerEntity player, net.minecraft.item.Item item) {
        for (ItemStack stack : player.getInventory().main) {
            if (stack.isOf(item)) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().offHand) {
            if (stack.isOf(item)) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().armor) {
            if (stack.isOf(item)) {
                return true;
            }
        }
        return false;
    }

    private static int countItem(ServerPlayerEntity player, net.minecraft.item.Item item) {
        int count = 0;
        for (ItemStack stack : player.getInventory().main) {
            if (stack.isOf(item)) {
                count += stack.getCount();
            }
        }
        for (ItemStack stack : player.getInventory().offHand) {
            if (stack.isOf(item)) {
                count += stack.getCount();
            }
        }
        for (ItemStack stack : player.getInventory().armor) {
            if (stack.isOf(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static int countGemItems(java.util.List<ItemStack> stacks) {
        int gemItems = 0;
        for (ItemStack stack : stacks) {
            if (stack.getItem() instanceof com.feel.gems.item.GemItem) {
                gemItems++;
            }
        }
        return gemItems;
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
    public void fluxChargeConsumesExactlyOneItem(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d startPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        player.teleport(world, startPos.x, startPos.y, startPos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.FLUX);
        GemPlayerState.setEnergy(player, 10);
        GemPowers.sync(player);

        player.setStackInHand(Hand.OFF_HAND, new ItemStack(Items.DIAMOND_BLOCK, 3));
        FluxCharge.set(player, 0);

        context.runAtTick(2L, () -> {
            int before = player.getOffHandStack().getCount();
            boolean ok = FluxCharge.tryConsumeChargeItem(player);
            int after = player.getOffHandStack().getCount();

            if (!ok) {
                context.throwGameTestException("Flux charge did not consume a valid charge item");
            }
            if (after != before - 1) {
                context.throwGameTestException("Expected charge to consume exactly 1 item, before=" + before + " after=" + after);
            }
            if (FluxCharge.get(player) <= 0) {
                context.throwGameTestException("Flux charge did not increase after consuming a charge item");
            }
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void heartLockDoesNotOscillate(TestContext context) {
        ServerWorld world = context.getWorld();

        ServerPlayerEntity caster = context.createMockCreativeServerPlayerInWorld();
        caster.changeGameMode(GameMode.SURVIVAL);

        ServerPlayerEntity target = context.createMockCreativeServerPlayerInWorld();
        target.changeGameMode(GameMode.SURVIVAL);

        Vec3d startPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        caster.teleport(world, startPos.x, startPos.y, startPos.z, 0.0F, 0.0F);
        target.teleport(world, startPos.x + 1.0D, startPos.y, startPos.z, 0.0F, 0.0F);

        target.setHealth(6.0F); // lock to 3 hearts (6 health points)
        AbilityRuntime.startHeartLock(caster, target, 120);

        float lockedMax = target.getHealth();
        long[] checks = new long[]{25L, 45L, 65L, 85L};
        for (long tick : checks) {
            context.runAtTick(tick, () -> {
                double max = target.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH);
                if (Math.abs(max - lockedMax) > 0.01D) {
                    context.throwGameTestException("Heart Lock oscillated: expected max=" + lockedMax + " got max=" + max);
                }
            });
        }
        context.runAtTick(110L, context::complete);
    }
}
