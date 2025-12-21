package com.feel.gems;

import com.feel.gems.state.GemPlayerState;
import com.feel.gems.item.ModItems;
import com.feel.gems.item.GemItemGlint;
import com.feel.gems.item.GemKeepOnDeath;
import com.feel.gems.item.GemOwnership;
import com.feel.gems.net.GemStateSync;
import com.feel.gems.power.FluxCharge;
import com.feel.gems.power.AbilityRuntime;
import com.feel.gems.power.BeaconAuraRuntime;
import com.feel.gems.power.BeaconSupportRuntime;
import com.feel.gems.power.BreezyBashTracker;
import com.feel.gems.power.GemPowers;
import com.feel.gems.power.SoulSystem;
import com.feel.gems.power.AutoSmeltCache;
import com.feel.gems.power.PowerIds;
import com.feel.gems.power.SummonerCommanderMark;
import com.feel.gems.power.SummonerSummons;
import com.feel.gems.power.SpaceAnomalies;
import com.feel.gems.power.PillagerVolleyRuntime;
import com.feel.gems.power.PillagerVindicatorBreakAbility;
import com.feel.gems.power.PillagerDiscipline;
import com.feel.gems.power.SpyMimicSystem;
import com.feel.gems.power.SpeedFrictionlessSteps;
import com.feel.gems.trust.GemTrust;
import com.feel.gems.debug.GemsStressTest;
import com.feel.gems.debug.GemsPerfMonitor;
import com.feel.gems.assassin.AssassinState;
import com.feel.gems.assassin.AssassinTeams;
import com.feel.gems.config.GemsBalance;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.ActionResult;
import com.feel.gems.core.GemId;

import java.util.ArrayList;
import java.util.List;

public final class GemsModEvents {
    private static int tickCounter = 0;

    private GemsModEvents() {
    }

    public static void register() {
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killedEntity) -> {
            if (entity instanceof ServerPlayerEntity player && killedEntity instanceof net.minecraft.entity.LivingEntity living) {
                SoulSystem.onKilledMob(player, living);
                SpyMimicSystem.recordLastKilledMob(player, living);

                if (!(killedEntity instanceof ServerPlayerEntity) && GemPowers.isPassiveActive(player, PowerIds.REAPER_HARVEST)) {
                    int dur = GemsBalance.v().reaper().harvestRegenDurationTicks();
                    int amp = GemsBalance.v().reaper().harvestRegenAmplifier();
                    if (dur > 0) {
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, dur, amp, true, false, false));
                    }
                }
            }
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient) {
                return ActionResult.PASS;
            }
            if (!(player instanceof ServerPlayerEntity sp)) {
                return ActionResult.PASS;
            }
            if (!(entity instanceof LivingEntity living)) {
                return ActionResult.PASS;
            }

            // Summoner: commander's mark (requires sword hit).
            if (sp.getStackInHand(hand).getItem() instanceof SwordItem && GemPowers.isPassiveActive(sp, PowerIds.SUMMONER_COMMANDERS_MARK)) {
                if (!(living instanceof ServerPlayerEntity other && GemTrust.isTrusted(sp, other))) {
                    int duration = GemsBalance.v().summoner().commandersMarkDurationTicks();
                    SummonerCommanderMark.mark(sp, living, duration);

                    int range = GemsBalance.v().summoner().commandRangeBlocks();
                    int strengthAmp = GemsBalance.v().summoner().commandersMarkStrengthAmplifier();
                    SummonerSummons.commandSummons(sp, living, range, strengthAmp, duration);
                }
            }

            // Pillager: shieldbreaker + Vindicator Break shield disabling.
            if (living instanceof ServerPlayerEntity victim && victim.isBlocking()) {
                long now = com.feel.gems.util.GemsTime.now(sp);
                boolean breakBuff = PillagerVindicatorBreakAbility.isActive(sp, now);
                boolean shieldbreaker = GemPowers.isPassiveActive(sp, PowerIds.PILLAGER_SHIELDBREAKER);
                if (breakBuff || shieldbreaker) {
                    victim.disableShield();
                    int cooldown = breakBuff
                            ? GemsBalance.v().pillager().vindicatorBreakShieldDisableCooldownTicks()
                            : GemsBalance.v().pillager().shieldbreakerDisableCooldownTicks();
                    if (cooldown > 0) {
                        victim.getItemCooldownManager().set(Items.SHIELD, cooldown);
                    }
                }
            }
            return ActionResult.PASS;
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            GemPlayerState.initIfNeeded(player);
            AssassinState.initIfNeeded(player);
            GemPlayerState.applyMaxHearts(player);
            GemOwnership.consumeOfflinePenalty(player);
            GemKeepOnDeath.restore(player);
            ensureActiveGemItem(player);
            GemPowers.sync(player);
            GemItemGlint.sync(player);
            GemStateSync.send(player);
            unlockStartingRecipes(server, player);
            AssassinTeams.sync(server, player);

            if (AssassinState.isEliminated(player)) {
                player.changeGameMode(net.minecraft.world.GameMode.SPECTATOR);
                player.sendMessage(net.minecraft.text.Text.literal("You have been eliminated as an assassin.").formatted(net.minecraft.util.Formatting.RED), false);
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            AbilityRuntime.cleanupOnDisconnect(server, player);
            GemTrust.clearRuntimeCache(player.getUuid());
            SummonerSummons.discardAll(player);
            PillagerVolleyRuntime.stop(player);
        });

        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            GemPlayerState.copy(oldPlayer, newPlayer);
            GemPlayerState.initIfNeeded(newPlayer);
            AssassinState.initIfNeeded(newPlayer);
            if (!alive) {
                // Death: bump gem epoch to invalidate old gem items and purge owned stacks.
                GemPlayerState.bumpGemEpoch(newPlayer);
                purgeOwnedGems(newPlayer);
            }
            GemPlayerState.applyMaxHearts(newPlayer);
            GemKeepOnDeath.restore(newPlayer);
            ensureActiveGemItem(newPlayer);
            GemPowers.sync(newPlayer);
            GemItemGlint.sync(newPlayer);
            GemStateSync.send(newPlayer);
            unlockStartingRecipes(newPlayer.getServer(), newPlayer);
            AssassinTeams.sync(newPlayer.getServer(), newPlayer);

            if (AssassinState.isEliminated(newPlayer)) {
                newPlayer.changeGameMode(net.minecraft.world.GameMode.SPECTATOR);
                newPlayer.sendMessage(net.minecraft.text.Text.literal("You have been eliminated as an assassin.").formatted(net.minecraft.util.Formatting.RED), false);
            }
        });

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> AutoSmeltCache.clear());

        ServerTickEvents.START_SERVER_TICK.register(server -> GemsPerfMonitor.onTickStart());

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            boolean doMaintain = tickCounter % 40 == 0;
            boolean doFluxOvercharge = tickCounter % 20 == 0;
            if (!doMaintain && !doFluxOvercharge) {
                return;
            }

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (doMaintain) {
                    GemPowers.maintain(player);
                }
                if (doFluxOvercharge) {
                    GemPlayerState.initIfNeeded(player);
                    if (GemPlayerState.getActiveGem(player) == GemId.FLUX && GemPlayerState.getEnergy(player) > 0) {
                        FluxCharge.tickOvercharge(player);
                    }
                    AbilityRuntime.tickEverySecond(player);
                    PillagerDiscipline.tick(player);
                    SpyMimicSystem.tickEverySecond(player);
                    BeaconSupportRuntime.tickEverySecond(player);
                    BeaconAuraRuntime.tickEverySecond(player);
                }
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(BreezyBashTracker::tick);
        ServerTickEvents.END_SERVER_TICK.register(SpaceAnomalies::tick);
        ServerTickEvents.END_SERVER_TICK.register(PillagerVolleyRuntime::tick);
        ServerTickEvents.END_SERVER_TICK.register(SpeedFrictionlessSteps::tick);
        ServerTickEvents.END_SERVER_TICK.register(GemsStressTest::tick);
        ServerTickEvents.END_SERVER_TICK.register(server -> com.feel.gems.item.GemOwnership.tickPurgeQueue(server));
        ServerTickEvents.END_SERVER_TICK.register(server -> GemsPerfMonitor.onTickEnd());
    }

    private static void ensureActiveGemItem(ServerPlayerEntity player) {
        Item gemItem = ModItems.gemItem(GemPlayerState.getActiveGem(player));
        if (hasItem(player, gemItem)) {
            return;
        }
        ItemStack stack = new ItemStack(gemItem);
        com.feel.gems.item.GemOwnership.tagOwned(stack, player.getUuid(), GemPlayerState.getGemEpoch(player));
        player.giveItemStack(stack);
    }

    private static boolean hasItem(ServerPlayerEntity player, Item item) {
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

    private static void purgeOwnedGems(ServerPlayerEntity player) {
        if (player.getServer() == null) {
            return;
        }
        com.feel.gems.item.GemOwnership.requestDeferredPurge(player.getServer(), player.getUuid());
    }

    private static void unlockStartingRecipes(net.minecraft.server.MinecraftServer server, ServerPlayerEntity player) {
        if (server == null) {
            return;
        }
        var manager = server.getRecipeManager();
        List<Identifier> ids = List.of(
                Identifier.of(GemsMod.MOD_ID, "heart"),
                Identifier.of(GemsMod.MOD_ID, "energy_upgrade"),
                Identifier.of(GemsMod.MOD_ID, "trader")
        );

        List<RecipeEntry<?>> entries = new ArrayList<>(ids.size());
        for (Identifier id : ids) {
            manager.get(id).ifPresent(entries::add);
        }
        if (!entries.isEmpty()) {
            player.unlockRecipes(entries);
        }
    }
}
