package com.feel.gems.gametest;

import java.util.EnumSet;

import com.feel.gems.assassin.AssassinState;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemDefinition;
import com.feel.gems.core.GemEnergyState;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
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
import com.feel.gems.power.AbilityDisables;
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

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void abilityDisablesClearAndCooldownSnapshotPersists(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.AIR);
        GemPlayerState.setEnergy(player, 8);

        var abilities = GemRegistry.definition(GemId.AIR).abilities();
        int dashIndex = abilities.indexOf(PowerIds.AIR_DASH);
        if (dashIndex < 0) {
            context.throwGameTestException("AIR gem missing dash ability registration");
            return;
        }

        AbilityDisables.disable(player, PowerIds.AIR_DASH);
        GemPlayerState.setActiveGem(player, GemId.FIRE);
        GemPlayerState.setActiveGem(player, GemId.AIR);
        if (AbilityDisables.isDisabled(player, PowerIds.AIR_DASH)) {
            context.throwGameTestException("Ability disables should clear on gem switch");
            return;
        }

        context.runAtTick(2L, () -> {
            long now = GemsTime.now(player);
            GemAbilities.activateByIndex(player, dashIndex);

            long nextAllowed = GemAbilityCooldowns.nextAllowedTick(player, PowerIds.AIR_DASH);
            if (nextAllowed <= now) {
                context.throwGameTestException("Dash ability did not start cooldown after activation");
                return;
            }

            int remaining = GemAbilityCooldowns.remainingTicks(player, PowerIds.AIR_DASH, now);
            RegistryByteBuf buf = new RegistryByteBuf(Unpooled.buffer(), world.getRegistryManager());
            CooldownSnapshotPayload payload = new CooldownSnapshotPayload(GemId.AIR.ordinal(), java.util.List.of(remaining));
            CooldownSnapshotPayload.CODEC.encode(buf, payload);
            buf.readerIndex(0);
            CooldownSnapshotPayload decoded = CooldownSnapshotPayload.CODEC.decode(buf);

            if (decoded.remainingAbilityCooldownTicks().isEmpty()) {
                context.throwGameTestException("Cooldown snapshot payload lost cooldown entries");
                return;
            }
            if (!decoded.remainingAbilityCooldownTicks().get(0).equals(remaining)) {
                context.throwGameTestException("Cooldown snapshot payload did not preserve remaining ticks");
                return;
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

    private static int countGlint(ServerPlayerEntity player, net.minecraft.item.Item item) {
        int glint = 0;
        for (ItemStack stack : player.getInventory().main) {
            if (stack.isOf(item) && stack.contains(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE)) {
                glint++;
            }
        }
        for (ItemStack stack : player.getInventory().offHand) {
            if (stack.isOf(item) && stack.contains(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE)) {
                glint++;
            }
        }
        for (ItemStack stack : player.getInventory().armor) {
            if (stack.isOf(item) && stack.contains(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE)) {
                glint++;
            }
        }
        return glint;
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

    private static boolean containsAirMace(ServerPlayerEntity player) {
        for (ItemStack stack : player.getInventory().main) {
            if (isAirMace(stack)) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().offHand) {
            if (isAirMace(stack)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAirMace(ItemStack stack) {
        if (!stack.isOf(Items.MACE)) {
            return false;
        }
        var custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        return custom != null && custom.getNbt().getBoolean("gemsAirMace");
    }

    private static void resetAssassinState(ServerPlayerEntity player) {
        AssassinState.initIfNeeded(player);
        var data = ((com.feel.gems.state.GemsPersistentDataHolder) player).gems$getPersistentData();
        data.putBoolean("assassinIsAssassin", false);
        data.putBoolean("assassinEliminated", false);
        data.putInt("assassinHearts", AssassinState.ASSASSIN_MAX_HEARTS);
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

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void assassinConversionAndHeartsApplied(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d startPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        player.teleport(world, startPos.x, startPos.y, startPos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        AssassinState.initIfNeeded(player);

        // Force conversion using the same persistent path the death mixin uses.
        AssassinState.becomeAssassin(player);
        AssassinState.addAssassinHearts(player, -2); // 8 hearts
        GemPlayerState.applyMaxHearts(player);

        context.runAtTick(2L, () -> {
            if (!AssassinState.isAssassin(player)) {
                context.throwGameTestException("Expected player to be assassin after becomeAssassin");
            }
            if (AssassinState.getAssassinHearts(player) != 8) {
                context.throwGameTestException("Expected assassin hearts to be 8, got " + AssassinState.getAssassinHearts(player));
            }
            double maxHealth = player.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH);
            if (Math.abs(maxHealth - 16.0D) > 0.01D) {
                context.throwGameTestException("Expected max health 16.0 for 8 hearts, got " + maxHealth);
            }
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void panicRingSpawnsConfiguredTnt(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        player.teleport(world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        int expected = GemsBalance.v().terror().panicRingTntCount();
        context.runAtTick(2L, () -> {
            boolean ok = new PanicRingAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Panic Ring did not activate");
            }
        });

        context.runAtTick(10L, () -> {
            Box box = new Box(player.getBlockPos()).expand(6.0D);
            int found = world.getEntitiesByClass(TntEntity.class, box, e -> true).size();
            if (found < expected) {
                context.throwGameTestException("Expected at least " + expected + " primed TNT, found " + found);
            }
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 400)
    public void summonerSummonsHaveNoDropsAndRecallWorks(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        player.teleport(world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.SUMMONER);
        GemPlayerState.setEnergy(player, 10);
        GemPowers.sync(player);

        int expected = GemsBalance.v().summoner().slot1().stream().mapToInt(s -> s.count).sum();
        context.runAtTick(2L, () -> {
            boolean ok = new SummonSlotAbility(1).activate(player);
            if (!ok) {
                context.throwGameTestException("Summon slot 1 did not activate");
            }
        });

        context.runAtTick(20L, () -> {
            Box box = new Box(player.getBlockPos()).expand(12.0D);
            var summons = world.getEntitiesByClass(net.minecraft.entity.mob.MobEntity.class, box,
                    e -> SummonerSummons.isSummon(e) && player.getUuid().equals(SummonerSummons.ownerUuid(e)));
            if (summons.size() < expected) {
                context.throwGameTestException("Expected at least " + expected + " summons, found " + summons.size());
            }

            // Kill one summon and ensure it drops no items or XP.
            var mob = summons.getFirst();
            mob.damage(mob.getDamageSources().outOfWorld(), 10_000.0F);

            context.runAtTick(40L, () -> {
                Box lootBox = new Box(player.getBlockPos()).expand(12.0D);
                int items = world.getEntitiesByClass(ItemEntity.class, lootBox, e -> true).size();
                int xp = world.getEntitiesByClass(ExperienceOrbEntity.class, lootBox, e -> true).size();
                if (items > 0) {
                    context.throwGameTestException("Summons dropped items unexpectedly (" + items + ")");
                }
                if (xp > 0) {
                    context.throwGameTestException("Summons dropped XP unexpectedly (" + xp + ")");
                }

                boolean recalled = new SummonRecallAbility().activate(player);
                if (!recalled) {
                    context.throwGameTestException("Recall did not activate");
                }
                context.runAtTick(60L, () -> {
                    int after = world.getEntitiesByClass(net.minecraft.entity.mob.MobEntity.class, lootBox,
                            e -> SummonerSummons.isSummon(e) && player.getUuid().equals(SummonerSummons.ownerUuid(e))).size();
                    if (after != 0) {
                        context.throwGameTestException("Expected all summons to be recalled, remaining=" + after);
                    }
                    context.complete();
                });
            });
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void recipesAreRegistered(TestContext context) {
        ServerWorld world = context.getWorld();
        var server = world.getServer();
        if (server == null) {
            context.throwGameTestException("No server instance");
            return;
        }
        var manager = server.getRecipeManager();
        for (String path : new String[]{"heart", "energy_upgrade", "trader"}) {
            Identifier id = Identifier.of("gems", path);
            if (manager.get(id).isEmpty()) {
                context.throwGameTestException("Missing recipe: " + id);
                return;
            }
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void pillagerFangsSpawnsFangs(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d startPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        player.teleport(world, startPos.x, startPos.y, startPos.z, 0.0F, 0.0F);

        var target = EntityType.ARMOR_STAND.create(world);
        if (target == null) {
            context.throwGameTestException("Failed to create target entity");
            return;
        }
        Vec3d targetPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 1.5D));
        target.refreshPositionAndAngles(targetPos.x, targetPos.y, targetPos.z, 0.0F, 0.0F);
        target.setNoGravity(true);
        world.spawnEntity(target);

        // Aim at the target.
        Vec3d aimAt = new Vec3d(target.getX(), target.getEyeY(), target.getZ());
        double dx = aimAt.x - player.getX();
        double dz = aimAt.z - player.getZ();
        double dy = aimAt.y - player.getEyeY();
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
        player.teleport(world, startPos.x, startPos.y, startPos.z, yaw, pitch);

        context.runAtTick(5L, () -> {
            boolean activated = new PillagerFangsAbility().activate(player);
            if (!activated) {
                context.throwGameTestException("Fangs did not activate");
                return;
            }

            Box box = new Box(target.getBlockPos()).expand(24.0D);
            int fangs = world.getEntitiesByClass(EvokerFangsEntity.class, box, e -> true).size();
            if (fangs <= 0) {
                context.throwGameTestException("Expected fangs entities to be spawned");
                return;
            }
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 220)
    public void spyStillnessCloakAppliesInvisibility(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d startPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        player.teleport(world, startPos.x, startPos.y, startPos.z, 0.0F, 0.0F);

        context.runAtTick(10L, () -> {
            GemPlayerState.initIfNeeded(player);
            GemPlayerState.setActiveGem(player, GemId.SPY_MIMIC);
            GemPlayerState.setEnergy(player, 5);
        });

        // Tick the stillness cloak logic deterministically.
        for (long t = 20L; t <= 140L; t += 20L) {
            long at = t;
            context.runAtTick(at, () -> SpyMimicSystem.tickEverySecond(player));
        }

        context.runAtTick(160L, () -> {
            if (!player.hasStatusEffect(StatusEffects.INVISIBILITY)) {
                context.throwGameTestException("Expected Stillness Cloak to apply invisibility");
                return;
            }
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void spyStealDisablesVictimAbility(TestContext context) {
        ServerWorld world = context.getWorld();
        var server = world.getServer();
        if (server == null) {
            context.throwGameTestException("No server instance");
            return;
        }

        ServerPlayerEntity spy = context.createMockCreativeServerPlayerInWorld();
        ServerPlayerEntity victim = context.createMockCreativeServerPlayerInWorld();
        spy.changeGameMode(GameMode.SURVIVAL);
        victim.changeGameMode(GameMode.SURVIVAL);

        Vec3d spyPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        Vec3d victimPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 1.5D));
        // Yaw 0 faces toward +Z (victim is in front).
        spy.teleport(world, spyPos.x, spyPos.y, spyPos.z, 0.0F, 0.0F);
        victim.teleport(world, victimPos.x, victimPos.y, victimPos.z, 180.0F, 0.0F);

        context.runAtTick(10L, () -> {
            GemPlayerState.initIfNeeded(spy);
            GemPlayerState.setActiveGem(spy, GemId.SPY_MIMIC);
            GemPlayerState.setEnergy(spy, 5);

            GemPlayerState.initIfNeeded(victim);
            GemPlayerState.setActiveGem(victim, GemId.ASTRA);
            GemPlayerState.setEnergy(victim, 5);
        });

        Identifier stolen = com.feel.gems.power.PowerIds.ASTRAL_DAGGERS;

        context.runAtTick(20L, () -> {
            // Simulate the victim casting an ability in front of the spy enough times.
            for (int i = 0; i < GemsBalance.v().spyMimic().stealRequiredWitnessCount(); i++) {
                SpyMimicSystem.onAbilityUsed(server, victim, stolen);
            }

            boolean ok = new SpyStealAbility().activate(spy);
            if (!ok) {
                context.throwGameTestException("Steal did not activate");
                return;
            }
            if (!AbilityDisables.isDisabled(victim, stolen)) {
                context.throwGameTestException("Expected victim ability to be disabled after steal");
                return;
            }
            if (!stolen.equals(SpyMimicSystem.selectedStolenAbility(spy))) {
                context.throwGameTestException("Expected stolen ability to be selected after steal");
                return;
            }
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void airDashAppliesVelocityAndIFrames(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        player.teleport(world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        context.runAtTick(2L, () -> {
            boolean ok = new AirDashAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Air Dash did not activate");
            }
        });

        context.runAtTick(8L, () -> {
            Vec3d vel = player.getVelocity();
            if (vel.lengthSquared() < 0.01D) {
                context.throwGameTestException("Dash did not apply forward velocity");
                return;
            }
            var res = player.getStatusEffect(StatusEffects.RESISTANCE);
            if (res == null || res.getDuration() <= 0) {
                context.throwGameTestException("Dash did not grant temporary i-frames");
                return;
            }
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void airWindJumpResetsFallAndLaunchesUp(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        player.teleport(world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        context.runAtTick(2L, () -> {
            player.fallDistance = 10.0F;
            boolean ok = new AirWindJumpAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Wind Jump did not activate");
            }
        });

        context.runAtTick(8L, () -> {
            if (player.fallDistance != 0.0F) {
                context.throwGameTestException("Wind Jump did not reset fall distance");
                return;
            }
            if (player.getVelocity().y <= 0.0D) {
                context.throwGameTestException("Wind Jump did not add upward velocity");
                return;
            }
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void airUpdraftLiftsTrustedAndDamagesEnemies(TestContext context) {
        ServerWorld world = context.getWorld();

        ServerPlayerEntity caster = context.createMockCreativeServerPlayerInWorld();
        caster.changeGameMode(GameMode.SURVIVAL);

        ServerPlayerEntity ally = context.createMockCreativeServerPlayerInWorld();
        ally.changeGameMode(GameMode.SURVIVAL);

        ServerPlayerEntity enemy = context.createMockCreativeServerPlayerInWorld();
        enemy.changeGameMode(GameMode.SURVIVAL);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        caster.teleport(world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        ally.teleport(world, pos.x + 1.0D, pos.y, pos.z, 0.0F, 0.0F);
        enemy.teleport(world, pos.x - 1.0D, pos.y, pos.z, 180.0F, 0.0F);

        GemTrust.trust(caster, ally.getUuid());
        final float enemyHealthBefore = enemy.getHealth();

        context.runAtTick(2L, () -> {
            boolean ok = new AirUpdraftZoneAbility().activate(caster);
            if (!ok) {
                context.throwGameTestException("Updraft Zone did not activate");
            }
        });

        context.runAtTick(10L, () -> {
            if (!ally.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
                context.throwGameTestException("Trusted ally did not receive slow falling");
                return;
            }
            if (ally.getVelocity().y <= 0.0D) {
                context.throwGameTestException("Trusted ally was not lifted by updraft");
                return;
            }
            if (enemy.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
                context.throwGameTestException("Enemy should not receive ally buff effects");
                return;
            }
            if (enemy.getHealth() >= enemyHealthBefore) {
                context.throwGameTestException("Enemy did not take damage from updraft zone");
                return;
            }
            if (enemy.getVelocity().lengthSquared() <= 0.0D) {
                context.throwGameTestException("Enemy did not receive knockback/launch");
                return;
            }
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 220)
    public void beaconAuraAppliesOnlyToTrusted(TestContext context) {
        ServerWorld world = context.getWorld();

        ServerPlayerEntity beacon = context.createMockCreativeServerPlayerInWorld();
        beacon.changeGameMode(GameMode.SURVIVAL);

        ServerPlayerEntity trusted = context.createMockCreativeServerPlayerInWorld();
        trusted.changeGameMode(GameMode.SURVIVAL);

        ServerPlayerEntity untrusted = context.createMockCreativeServerPlayerInWorld();
        untrusted.changeGameMode(GameMode.SURVIVAL);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        beacon.teleport(world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        trusted.teleport(world, pos.x + 1.5D, pos.y, pos.z, 0.0F, 0.0F);
        untrusted.teleport(world, pos.x + 3.0D, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(beacon);
        GemPlayerState.setActiveGem(beacon, GemId.BEACON);
        GemPlayerState.setEnergy(beacon, 10);
        GemPowers.sync(beacon);
        GemTrust.trust(beacon, trusted.getUuid());

        context.runAtTick(4L, () -> {
            boolean ok = new BeaconAuraAbility(BeaconAuraRuntime.AuraType.SPEED).activate(beacon);
            if (!ok) {
                context.throwGameTestException("Beacon aura did not activate");
            }
        });

        for (long tick = 8L; tick <= 32L; tick += 8L) {
            long at = tick;
            context.runAtTick(at, () -> BeaconAuraRuntime.tickEverySecond(beacon));
        }

        context.runAtTick(40L, () -> {
            var eff = trusted.getStatusEffect(StatusEffects.SPEED);
            if (eff == null) {
                context.throwGameTestException("Trusted player did not receive beacon aura");
                return;
            }
            int expectedAmp = GemsBalance.v().beacon().auraSpeedAmplifier();
            if (eff.getAmplifier() != expectedAmp) {
                context.throwGameTestException("Unexpected aura amplifier: " + eff.getAmplifier() + " expected=" + expectedAmp);
                return;
            }
            if (untrusted.hasStatusEffect(StatusEffects.SPEED)) {
                context.throwGameTestException("Untrusted player should not receive beacon aura");
                return;
            }
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 260)
    public void pillagerVolleyFiresAndStopsWhenEnergyGone(TestContext context) {
        ServerWorld world = context.getWorld();
        var server = world.getServer();
        if (server == null) {
            context.throwGameTestException("No server instance");
            return;
        }

        final int[] arrowsAtStop = new int[1];

        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        player.teleport(world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.PILLAGER);
        GemPlayerState.setEnergy(player, 10);
        GemPowers.sync(player);

        context.runAtTick(2L, () -> {
            boolean ok = new PillagerVolleyAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Pillager Volley did not activate");
            }
        });

        for (long tick = 5L; tick <= 80L; tick += 10L) {
            long at = tick;
            context.runAtTick(at, () -> PillagerVolleyRuntime.tick(server));
        }

        context.runAtTick(90L, () -> {
            Box box = new Box(player.getBlockPos()).expand(24.0D);
            int arrows = world.getEntitiesByClass(ArrowEntity.class, box, e -> true).size();
            if (arrows <= 0) {
                context.throwGameTestException("Volley did not spawn arrows");
                return;
            }
            arrowsAtStop[0] = arrows;
        });

        context.runAtTick(100L, () -> GemPlayerState.setEnergy(player, 0));
        for (long tick = 110L; tick <= 200L; tick += 20L) {
            long at = tick;
            context.runAtTick(at, () -> PillagerVolleyRuntime.tick(server));
        }

        context.runAtTick(220L, () -> {
            Box box = new Box(player.getBlockPos()).expand(24.0D);
            int arrowsAfter = world.getEntitiesByClass(ArrowEntity.class, box, e -> true).size();
            // Energy drop should stop spawning new arrows; count should stay stable after the stop tick window.
            if (arrowsAfter != arrowsAtStop[0]) {
                context.throwGameTestException("Volley continued firing after energy hit zero (before=" + arrowsAtStop[0] + " after=" + arrowsAfter + ")");
                return;
            }
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void pillagerDisciplineTriggersBelowThreshold(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        player.teleport(world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.PILLAGER);
        GemPlayerState.setEnergy(player, 10);

        context.runAtTick(2L, () -> {
            player.setHealth(20.0F);
            PillagerDiscipline.tick(player);
            if (player.hasStatusEffect(StatusEffects.RESISTANCE)) {
                context.throwGameTestException("Discipline should not trigger at high health");
            }
            player.setHealth(6.0F);
            PillagerDiscipline.tick(player);
        });

        context.runAtTick(40L, () -> {
            var res = player.getStatusEffect(StatusEffects.RESISTANCE);
            if (res == null || res.getDuration() <= 0) {
                context.throwGameTestException("Discipline did not grant resistance when low health");
                return;
            }
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 340)
    public void spyMimicFormAppliesAndCleansUp(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity spy = context.createMockCreativeServerPlayerInWorld();
        spy.changeGameMode(GameMode.SURVIVAL);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        spy.teleport(world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        var pig = EntityType.PIG.create(world);
        if (pig == null) {
            context.throwGameTestException("Failed to spawn pig to mimic");
            return;
        }
        pig.refreshPositionAndAngles(spy.getX(), spy.getY(), spy.getZ(), 0.0F, 0.0F);
        world.spawnEntity(pig);

        GemPlayerState.initIfNeeded(spy);
        GemPlayerState.setActiveGem(spy, GemId.SPY_MIMIC);
        GemPlayerState.setEnergy(spy, 10);

        context.runAtTick(10L, () -> {
            SpyMimicSystem.recordLastKilledMob(spy, pig);
            pig.discard();
            boolean ok = new SpyMimicFormAbility().activate(spy);
            if (!ok) {
                context.throwGameTestException("Mimic Form did not activate after recording kill");
            }
        });

        context.runAtTick(20L, () -> {
            double baseMax = spy.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH);
            spy.damage(spy.getDamageSources().outOfWorld(), 0.0F); // force attribute sync
            if (baseMax <= 20.0D) {
                context.throwGameTestException("Mimic Form did not increase max health");
                return;
            }
            if (!spy.hasStatusEffect(StatusEffects.INVISIBILITY)) {
                context.throwGameTestException("Mimic Form should grant invisibility during the form");
                return;
            }
        });

        for (long tick = 40L; tick <= 260L; tick += 20L) {
            long at = tick;
            context.runAtTick(at, () -> SpyMimicSystem.tickEverySecond(spy));
        }

        context.runAtTick(300L, () -> {
            double after = spy.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH);
            if (after > 20.1D) {
                context.throwGameTestException("Mimic Form buffs did not clear");
                return;
            }
            if (spy.hasStatusEffect(StatusEffects.INVISIBILITY)) {
                context.throwGameTestException("Mimic Form invisibility should expire after duration");
                return;
            }
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 120)
    public void unlockOrderFollowsSpec(TestContext context) {
        // Use Fire as a representative 4-ability gem and also validate all gems respect the energy spec.
        GemDefinition fire = GemRegistry.definition(GemId.FIRE);
        int fireAbilities = fire.abilities().size();
        if (fireAbilities < 3) {
            context.throwGameTestException("Fire gem should have at least 3 abilities for unlock progression test");
            return;
        }

        int at2 = fire.availableAbilities(new GemEnergyState(2)).size();
        int at3 = fire.availableAbilities(new GemEnergyState(3)).size();
        int at4 = fire.availableAbilities(new GemEnergyState(4)).size();
        int at5 = fire.availableAbilities(new GemEnergyState(5)).size();

        if (at2 != 1 || at3 != Math.min(2, fireAbilities) || at4 != Math.min(3, fireAbilities) || at5 != fireAbilities) {
            context.throwGameTestException("Fire unlock counts did not follow spec: " + at2 + "," + at3 + "," + at4 + "," + at5);
            return;
        }

        // All gems should follow the same energy unlock curve.
        for (GemId id : GemId.values()) {
            GemDefinition def = GemRegistry.definition(id);
            if (def == null) {
                continue;
            }
            int total = def.abilities().size();
            int e2 = def.availableAbilities(new GemEnergyState(2)).size();
            int e3 = def.availableAbilities(new GemEnergyState(3)).size();
            int e4 = def.availableAbilities(new GemEnergyState(4)).size();
            int e5 = def.availableAbilities(new GemEnergyState(5)).size();

            if (e2 != Math.min(1, total) || e3 != Math.min(2, total) || e4 != Math.min(3, total) || e5 != total) {
                context.throwGameTestException("Unlock curve mismatch for gem " + id + " counts=" + e2 + "," + e3 + "," + e4 + "," + e5 + " total=" + total);
                return;
            }
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 120)
    public void energyLadderGatesAbilitiesAndLosesOnDeath(TestContext context) {
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.FIRE);
        GemDefinition def = GemRegistry.definition(GemId.FIRE);
        int total = def.abilities().size();

        int e0 = def.availableAbilities(new GemEnergyState(0)).size();
        int e1 = def.availableAbilities(new GemEnergyState(1)).size();
        int e2 = def.availableAbilities(new GemEnergyState(2)).size();
        int e4 = def.availableAbilities(new GemEnergyState(4)).size();
        int e5 = def.availableAbilities(new GemEnergyState(5)).size();

        if (e0 != 0 || e1 != 0 || e2 != Math.min(1, total) || e4 != Math.min(3, total) || e5 != total) {
            context.throwGameTestException("Ability gating mismatch for Fire gem");
            return;
        }

        if (GemPlayerState.getEnergy(player) != GemPlayerState.DEFAULT_ENERGY) {
            context.throwGameTestException("Default energy should start at 3");
            return;
        }

        GemPlayerState.addEnergy(player, 1);
        if (GemPlayerState.getEnergy(player) != 4) {
            context.throwGameTestException("Kill-style energy gain should increase energy by 1 to 4");
            return;
        }

        GemPlayerState.addEnergy(player, -1); // simulate death loss
        if (GemPlayerState.getEnergy(player) != 3) {
            context.throwGameTestException("Death should reduce energy by 1 to 3");
            return;
        }

        context.complete();
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
        int tradersBefore = countItem(player, ModItems.TRADER);

        GemTrading.Result ok = GemTrading.trade(player, GemId.FLUX);
        if (!ok.success() || !ok.consumedTrader()) {
            context.throwGameTestException("Trade should succeed and consume a trader item");
            return;
        }
        int tradersAfter = countItem(player, ModItems.TRADER);
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

        int gemItems = countGemItems(player.getInventory().main)
                + countGemItems(player.getInventory().offHand)
                + countGemItems(player.getInventory().armor);
        if (gemItems != 1 || !hasItem(player, ModItems.FLUX_GEM)) {
            context.throwGameTestException("Inventory should contain exactly one Flux gem item after trade");
            return;
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 160)
    public void heartDropsAboveFloorAndStopsAtFive(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity killer = context.createMockCreativeServerPlayerInWorld();
        ServerPlayerEntity victim = context.createMockCreativeServerPlayerInWorld();
        killer.changeGameMode(GameMode.SURVIVAL);
        victim.changeGameMode(GameMode.SURVIVAL);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        killer.teleport(world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        victim.teleport(world, pos.x + 1.0D, pos.y, pos.z, 180.0F, 0.0F);

        GemPlayerState.initIfNeeded(killer);
        GemPlayerState.initIfNeeded(victim);
        GemPlayerState.setActiveGem(killer, GemId.FIRE);
        GemPlayerState.setActiveGem(victim, GemId.LIFE);
        resetAssassinState(victim);
        GemPlayerState.setMaxHearts(victim, GemPlayerState.MIN_MAX_HEARTS + 1);
        GemPlayerState.applyMaxHearts(victim);

        context.runAtTick(2L, () -> {
            victim.setHealth(2.0F);
            victim.damage(victim.getDamageSources().playerAttack(killer), 10.0F);
            if (!victim.isDead()) {
                // Guarantee death triggers even if mock combat fails to deliver damage (e.g. PVP quirks).
                victim.kill();
            }
        });

        context.runAtTick(40L, () -> {
            if (!victim.isDead()) {
                context.throwGameTestException("Victim did not die during heart drop scenario");
                return;
            }
            int heartsDropped = world.getEntitiesByClass(ItemEntity.class, new Box(pos, pos.add(1.0D, 1.0D, 1.0D)).expand(3.0D), e -> e.getStack().isOf(ModItems.HEART)).size();
            int invHearts = countItem(killer, ModItems.HEART);
            var victimDataAfter = ((com.feel.gems.state.GemsPersistentDataHolder) victim).gems$getPersistentData();
            if (heartsDropped + invHearts != 1) {
                context.throwGameTestException("Expected exactly one heart gained (dropped or picked up), found dropped=" + heartsDropped + " inv=" + invHearts + " assassin=" + victimDataAfter.getBoolean("assassinIsAssassin") + " storedMax=" + victimDataAfter.getInt("maxHearts") + " assassinHearts=" + victimDataAfter.getInt("assassinHearts"));
                return;
            }
            int storedHearts = victimDataAfter.getInt("maxHearts");
            if (storedHearts != GemPlayerState.MIN_MAX_HEARTS) {
                context.throwGameTestException("Victim hearts should clamp to floor after drop: " + storedHearts);
                return;
            }
            boolean becameAssassin = victimDataAfter.getBoolean("assassinIsAssassin");
            if (becameAssassin) {
                context.throwGameTestException("Victim above floor should not convert to assassin");
                return;
            }
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 160)
    public void heartAtFloorDoesNotDropAndTurnsAssassin(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity killer = context.createMockCreativeServerPlayerInWorld();
        ServerPlayerEntity victim = context.createMockCreativeServerPlayerInWorld();
        killer.changeGameMode(GameMode.SURVIVAL);
        victim.changeGameMode(GameMode.SURVIVAL);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        killer.teleport(world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        victim.teleport(world, pos.x + 1.0D, pos.y, pos.z, 180.0F, 0.0F);

        GemPlayerState.initIfNeeded(killer);
        GemPlayerState.initIfNeeded(victim);
        GemPlayerState.setActiveGem(killer, GemId.FIRE);
        GemPlayerState.setActiveGem(victim, GemId.LIFE);
        resetAssassinState(victim);
        GemPlayerState.setMaxHearts(victim, GemPlayerState.MIN_MAX_HEARTS);
        GemPlayerState.applyMaxHearts(victim);

        context.runAtTick(2L, () -> {
            victim.setHealth(2.0F);
            victim.damage(victim.getDamageSources().playerAttack(killer), 10.0F);
            if (!victim.isDead()) {
                victim.kill();
            }
        });

        context.runAtTick(40L, () -> {
            if (!victim.isDead()) {
                context.throwGameTestException("Victim did not die during floor clamp scenario");
                return;
            }
            int heartsDropped = world.getEntitiesByClass(ItemEntity.class, new Box(pos, pos.add(1.0D, 1.0D, 1.0D)).expand(3.0D), e -> e.getStack().isOf(ModItems.HEART)).size();
            var victimDataAfter = ((com.feel.gems.state.GemsPersistentDataHolder) victim).gems$getPersistentData();
            if (heartsDropped != 0) {
                context.throwGameTestException("No hearts should drop at the floor (5 hearts)");
                return;
            }
            int invHearts = countItem(killer, ModItems.HEART);
            if (invHearts != 0) {
                context.throwGameTestException("Killer should not receive a heart when victim at floor");
                return;
            }
            boolean becameAssassin = victimDataAfter.getBoolean("assassinIsAssassin");
            if (!becameAssassin) {
                context.throwGameTestException("Victim at floor should be converted to assassin on death (storedMax=" + victimDataAfter.getInt("maxHearts") + " assassinHearts=" + victimDataAfter.getInt("assassinHearts") + ")");
                return;
            }
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 120)
    public void glintAppliesOnlyToActiveGemAtCap(TestContext context) {
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setEnergy(player, GemPlayerState.MAX_ENERGY);
        GemPlayerState.setActiveGem(player, GemId.ASTRA);

        player.getInventory().clear();
        player.giveItemStack(new ItemStack(ModItems.ASTRA_GEM));
        player.giveItemStack(new ItemStack(ModItems.FIRE_GEM));

        context.runAtTick(2L, () -> {
            GemItemGlint.sync(player);
            if (countGlint(player, ModItems.ASTRA_GEM) != 1) {
                context.throwGameTestException("Active gem should glint when at energy cap");
                return;
            }
            if (countGlint(player, ModItems.FIRE_GEM) != 0) {
                context.throwGameTestException("Inactive gems should not glint when another gem is active");
                return;
            }

            GemPlayerState.setEnergy(player, GemPlayerState.MIN_ENERGY);
            GemItemGlint.sync(player);
            if (countGlint(player, ModItems.ASTRA_GEM) != 0) {
                context.throwGameTestException("Glint should clear when energy drops below cap");
                return;
            }

            GemPlayerState.setActiveGem(player, GemId.FIRE);
            GemPlayerState.setEnergy(player, GemPlayerState.MAX_ENERGY);
            GemItemGlint.sync(player);
            if (countGlint(player, ModItems.ASTRA_GEM) != 0) {
                context.throwGameTestException("Old active gem should lose glint after switching");
                return;
            }
            if (countGlint(player, ModItems.FIRE_GEM) != 1) {
                context.throwGameTestException("New active gem should gain glint at energy cap");
                return;
            }
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 160)
    public void passivesApplyAndStopWhenDisabled(TestContext context) {
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.SPEED);
        GemPlayerState.setEnergy(player, 4);

        GemPowers.sync(player);
        if (!player.hasStatusEffect(StatusEffects.SPEED)) {
            context.throwGameTestException("Speed passive should apply when energy is above zero");
            return;
        }

        GemPlayerState.setEnergy(player, 0);
        GemPowers.sync(player);
        if (player.hasStatusEffect(StatusEffects.SPEED)) {
            context.throwGameTestException("Speed passive should clear when energy hits zero");
            return;
        }

        GemPlayerState.setActiveGem(player, GemId.AIR);
        GemPlayerState.setEnergy(player, 4);
        GemPowers.maintain(player);
        if (!containsAirMace(player)) {
            context.throwGameTestException("Maintained passive should grant air mace when enabled");
            return;
        }

        player.getInventory().clear();
        GemPlayerState.setEnergy(player, 0);
        GemPowers.maintain(player);
        if (containsAirMace(player)) {
            context.throwGameTestException("Maintained passives should stop ticking when disabled");
            return;
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 120)
    public void energyUpgradeIncreasesEnergyUntilCap(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setEnergy(player, GemPlayerState.MAX_ENERGY - 1);

        ItemStack upgrades = new ItemStack(ModItems.ENERGY_UPGRADE, 2);
        player.setStackInHand(Hand.MAIN_HAND, upgrades);

        context.runAtTick(2L, () -> {
            ModItems.ENERGY_UPGRADE.use(world, player, Hand.MAIN_HAND);
            if (GemPlayerState.getEnergy(player) != GemPlayerState.MAX_ENERGY) {
                context.throwGameTestException("Energy upgrade did not raise energy to cap");
                return;
            }
            if (player.getMainHandStack().getCount() != 1) {
                context.throwGameTestException("Energy upgrade should consume exactly one item when applied");
                return;
            }

            ModItems.ENERGY_UPGRADE.use(world, player, Hand.MAIN_HAND);
            if (GemPlayerState.getEnergy(player) != GemPlayerState.MAX_ENERGY) {
                context.throwGameTestException("Energy should stay capped after extra upgrade use");
                return;
            }
            if (player.getMainHandStack().getCount() != 1) {
                context.throwGameTestException("Energy upgrade should not consume when at cap");
                return;
            }
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 120)
    public void heartItemRespectsMaxCap(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setMaxHearts(player, GemPlayerState.MAX_MAX_HEARTS - 1);
        GemPlayerState.applyMaxHearts(player);

        ItemStack hearts = new ItemStack(ModItems.HEART, 2);
        player.setStackInHand(Hand.MAIN_HAND, hearts);

        context.runAtTick(2L, () -> {
            ModItems.HEART.use(world, player, Hand.MAIN_HAND);
            if (GemPlayerState.getMaxHearts(player) != GemPlayerState.MAX_MAX_HEARTS) {
                context.throwGameTestException("Heart did not increase max hearts to the cap");
                return;
            }
            if (player.getMainHandStack().getCount() != 1) {
                context.throwGameTestException("Heart should consume exactly one item when applied");
                return;
            }

            ModItems.HEART.use(world, player, Hand.MAIN_HAND);
            if (GemPlayerState.getMaxHearts(player) != GemPlayerState.MAX_MAX_HEARTS) {
                context.throwGameTestException("Hearts should stay at cap after extra use");
                return;
            }
            if (player.getMainHandStack().getCount() != 1) {
                context.throwGameTestException("Heart should not consume when already capped");
                return;
            }
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 120)
    public void pocketsInventoryPersistsAcrossSaves(TestContext context) {
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);

        context.runAtTick(2L, () -> {
            var pockets = com.feel.gems.screen.PocketsStorage.load(player);
            pockets.setStack(0, new ItemStack(Items.DIAMOND, 3));
            com.feel.gems.screen.PocketsStorage.save(player, pockets);

            var reloaded = com.feel.gems.screen.PocketsStorage.load(player);
            if (!ItemStack.areEqual(reloaded.getStack(0), new ItemStack(Items.DIAMOND, 3))) {
                context.throwGameTestException("Pockets inventory did not persist items through save/load");
                return;
            }
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void spyStealRespectsWitnessCountEvenOnColdCache(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity spy = context.createMockCreativeServerPlayerInWorld();
        ServerPlayerEntity caster = context.createMockCreativeServerPlayerInWorld();
        spy.changeGameMode(GameMode.SURVIVAL);
        caster.changeGameMode(GameMode.SURVIVAL);

        Vec3d spyPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        Vec3d casterPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 6.5D));
        spy.teleport(world, spyPos.x, spyPos.y, spyPos.z, 0.0F, 0.0F); // yaw 0 faces +Z
        caster.teleport(world, casterPos.x, casterPos.y, casterPos.z, 180.0F, 0.0F);

        GemPlayerState.initIfNeeded(spy);
        GemPlayerState.setActiveGem(spy, GemId.SPY_MIMIC);
        GemPlayerState.setEnergy(spy, 10);

        Identifier observed = PowerIds.FIREBALL;
        int required = GemsBalance.v().spyMimic().stealRequiredWitnessCount();
        long now = GemsTime.now(world);
        for (int i = 0; i < required; i++) {
            SpyMimicSystem.onAbilityUsed(world.getServer(), caster, observed);
        }

        int seen = SpyMimicSystem.witnessedCount(spy, observed);
        if (seen < required) {
            context.throwGameTestException("Spy did not record required observations; saw=" + seen + " required=" + required);
            return;
        }
        if (!SpyMimicSystem.canSteal(spy, observed, now)) {
            context.throwGameTestException("Spy cannot steal after required observations (cold cache path)");
            return;
        }

        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void summonerRecallCleansSummons(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity owner = context.createMockCreativeServerPlayerInWorld();
        owner.changeGameMode(GameMode.SURVIVAL);

        var zombie = EntityType.ZOMBIE.create(world);
        if (zombie == null) {
            context.throwGameTestException("Failed to create zombie summon");
            return;
        }
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        zombie.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        zombie.setPersistent();
        world.spawnEntity(zombie);

        long until = GemsTime.now(owner) + 200;
        SummonerSummons.mark(zombie, owner.getUuid(), until);
        SummonerSummons.trackSpawn(owner, zombie);

        int tracked = SummonerSummons.pruneAndCount(owner);
        if (tracked != 1) {
            context.throwGameTestException("Expected one tracked summon, got " + tracked);
            return;
        }

        SummonerSummons.discardAll(owner);
        if (SummonerSummons.findEntity(world.getServer(), zombie.getUuid()) != null) {
            context.throwGameTestException("Summon was not discarded on recall");
            return;
        }
        if (!SummonerSummons.ownedSummonUuids(owner).isEmpty()) {
            context.throwGameTestException("Owned summon list not cleared on recall");
            return;
        }

        context.complete();
    }
}
