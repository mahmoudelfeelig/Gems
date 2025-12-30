package com.feel.gems;

import com.feel.gems.assassin.AssassinState;
import com.feel.gems.assassin.AssassinTeams;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.debug.GemsPerfMonitor;
import com.feel.gems.debug.GemsStressTest;
import com.feel.gems.item.GemItemGlint;
import com.feel.gems.item.GemKeepOnDeath;
import com.feel.gems.item.GemOwnership;
import com.feel.gems.item.ModItems;
import com.feel.gems.item.legendary.RecallRelicItem;
import com.feel.gems.legendary.HypnoControl;
import com.feel.gems.legendary.LegendaryCrafting;
import com.feel.gems.legendary.LegendaryPlayerTracker;
import com.feel.gems.legendary.LegendaryTargeting;
import com.feel.gems.legendary.SupremeSetRuntime;
import com.feel.gems.net.GemStateSync;
import com.feel.gems.power.ability.pillager.PillagerVindicatorBreakAbility;
import com.feel.gems.power.gem.astra.SoulSystem;
import com.feel.gems.power.gem.beacon.BeaconAuraRuntime;
import com.feel.gems.power.gem.beacon.BeaconSupportRuntime;
import com.feel.gems.power.gem.fire.AutoSmeltCache;
import com.feel.gems.power.gem.flux.FluxCharge;
import com.feel.gems.power.gem.pillager.PillagerDiscipline;
import com.feel.gems.power.gem.pillager.PillagerVolleyRuntime;
import com.feel.gems.power.gem.puff.BreezyBashTracker;
import com.feel.gems.power.gem.space.SpaceAnomalies;
import com.feel.gems.power.gem.speed.SpeedFrictionlessSteps;
import com.feel.gems.power.gem.terror.TerrorRigRuntime;
import com.feel.gems.power.gem.spy.SpyMimicSystem;
import com.feel.gems.power.gem.summoner.SummonerCommanderMark;
import com.feel.gems.power.gem.summoner.SummonerSummons;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityRuntime;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.trust.GemTrust;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;




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
            if (entity instanceof ServerPlayerEntity killer && killedEntity instanceof ServerPlayerEntity victim) {
                com.feel.gems.legendary.LegendaryWeapons.onPlayerKill(killer, victim);
                SpyMimicSystem.restoreStolenOnKill(killer, victim);
            }
        });

        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            if (sender instanceof ServerPlayerEntity player) {
                if (SpyMimicSystem.isSkinshiftTarget(player)) {
                    player.sendMessage(Text.literal("You cannot chat while being skinshifted."), true);
                    return false;
                }
                Text disguise = SpyMimicSystem.chatDisguiseName(player);
                if (disguise != null) {
                    broadcastDisguisedChat(player, disguise, message.getContent());
                    return false;
                }
            }
            return true;
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

            // Summoner: commander's mark.
            if (GemPowers.isPassiveActive(sp, PowerIds.SUMMONER_COMMANDERS_MARK)) {
                if (!(living instanceof ServerPlayerEntity other && GemTrust.isTrusted(sp, other))) {
                    int duration = GemsBalance.v().summoner().commandersMarkDurationTicks();
                    SummonerCommanderMark.mark(sp, living, duration);

                    int range = GemsBalance.v().summoner().commandRangeBlocks();
                    int strengthAmp = GemsBalance.v().summoner().commandersMarkStrengthAmplifier();
                    SummonerSummons.commandSummons(sp, living, range, strengthAmp, duration);
                }
            }

            HypnoControl.commandMobs(sp, living, GemsBalance.v().legendary().hypnoRangeBlocks());

            LegendaryTargeting.recordHit(sp, living);

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

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient) {
                return ActionResult.PASS;
            }
            if (!(player instanceof ServerPlayerEntity sp)) {
                return ActionResult.PASS;
            }
            if (com.feel.gems.power.gem.terror.TerrorRemoteChargeRuntime.tryArm(sp, hitResult.getBlockPos())) {
                com.feel.gems.power.runtime.AbilityFeedback.sound(sp, net.minecraft.sound.SoundEvents.ENTITY_TNT_PRIMED, 0.8F, 1.2F);
                sp.sendMessage(net.minecraft.text.Text.literal("Remote charge armed."), true);
                return ActionResult.SUCCESS;
            }
            if (TerrorRigRuntime.tryTriggerUse(sp, hitResult.getBlockPos())) {
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (world.isClient) {
                return true;
            }
            if (player instanceof ServerPlayerEntity sp) {
                TerrorRigRuntime.tryTriggerBreak(sp, pos);
            }
            return true;
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
            SpyMimicSystem.syncSkinshifts(player);
            SpyMimicSystem.syncSkinshiftSelf(player);
            unlockStartingRecipes(server, player);
            AssassinTeams.sync(server, player);
            LegendaryCrafting.deliverPending(player);
            RecallRelicItem.ensureForceload(player);
            HypnoControl.releaseAll(player);

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
            HypnoControl.releaseAll(player);
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
            SpyMimicSystem.syncSkinshifts(newPlayer);
            SpyMimicSystem.syncSkinshiftSelf(newPlayer);
            unlockStartingRecipes(newPlayer.getServer(), newPlayer);
            AssassinTeams.sync(newPlayer.getServer(), newPlayer);
            LegendaryCrafting.deliverPending(newPlayer);
            RecallRelicItem.ensureForceload(newPlayer);
            HypnoControl.releaseAll(newPlayer);

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
                    SupremeSetRuntime.tick(player);
                    HypnoControl.pruneAndCount(player);
                    com.feel.gems.power.gem.astra.SoulSummons.pruneAndCount(player);
                    HypnoControl.followOwner(player);
                    com.feel.gems.power.gem.astra.SoulSummons.followOwner(player);
                    SummonerSummons.followOwner(player);
                    RecallRelicItem.clearIfMissingItem(player);
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

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (tickCounter % 20 == 0) {
                LegendaryCrafting.tick(server);
                LegendaryPlayerTracker.tick(server);
                TerrorRigRuntime.tick(server);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(BreezyBashTracker::tick);
        ServerTickEvents.END_SERVER_TICK.register(SpaceAnomalies::tick);
        ServerTickEvents.END_SERVER_TICK.register(PillagerVolleyRuntime::tick);
        ServerTickEvents.END_SERVER_TICK.register(SpeedFrictionlessSteps::tick);
        ServerTickEvents.END_SERVER_TICK.register(GemsStressTest::tick);
        ServerTickEvents.END_SERVER_TICK.register(server -> com.feel.gems.item.GemOwnership.tickPurgeQueue(server));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                TerrorRigRuntime.checkStep(player);
            }
        });
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

    private static void broadcastDisguisedChat(ServerPlayerEntity sender, Text disguise, Text content) {
        if (sender == null || sender.getServer() == null) {
            return;
        }
        Text message = Text.literal("<").append(disguise).append("> ").append(content);
        for (ServerPlayerEntity player : sender.getServer().getPlayerManager().getPlayerList()) {
            player.sendMessage(message, false);
        }
    }

    private static void purgeOwnedGems(ServerPlayerEntity player) {
        if (player.getServer() == null) {
            return;
        }
        com.feel.gems.item.GemOwnership.requestDeferredPurge(player.getServer(), player.getUuid());
    }

    public static void unlockStartingRecipes(net.minecraft.server.MinecraftServer server, ServerPlayerEntity player) {
        if (server == null) {
            return;
        }
        var manager = server.getRecipeManager();
        List<RecipeEntry<?>> entries = new ArrayList<>();
        for (RecipeEntry<?> entry : manager.values()) {
            if (GemsMod.MOD_ID.equals(entry.id().getNamespace())) {
                entries.add(entry);
            }
        }
        if (!entries.isEmpty()) {
            player.unlockRecipes(entries);
        }
    }
}
