package com.feel.gems;

import com.feel.gems.assassin.AssassinState;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.assassin.AssassinTeams;
import com.feel.gems.bounty.BountyBoard;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.config.GemsDisables;
import com.feel.gems.core.GemId;
import com.feel.gems.debug.GemsPerfMonitor;
import com.feel.gems.debug.GemsStressTest;
import com.feel.gems.item.GemItemGlint;
import com.feel.gems.mastery.TitleDisplay;
import com.feel.gems.legendary.LegendaryCooldowns;
import com.feel.gems.net.GemCooldownSync;
import com.feel.gems.item.GemKeepOnDeath;
import com.feel.gems.item.GemOwnership;
import com.feel.gems.item.ModItems;
import com.feel.gems.item.legendary.RecallRelicItem;
import com.feel.gems.legendary.HypnoControl;
import com.feel.gems.legendary.LegendaryCrafting;
import com.feel.gems.legendary.LegendaryPlayerTracker;
import com.feel.gems.legendary.LegendaryTargeting;
import com.feel.gems.legendary.LegendaryDuels;
import com.feel.gems.legendary.SupremeSetRuntime;
import com.feel.gems.mastery.MasteryAuraRuntime;
import com.feel.gems.net.GemStateSync;
import com.feel.gems.net.ServerDisablesPayload;
import com.feel.gems.rivalry.RivalryManager;
import com.feel.gems.power.ability.duelist.DuelistMirrorMatchRuntime;
import com.feel.gems.power.ability.pillager.PillagerVindicatorBreakAbility;
import com.feel.gems.power.ability.hunter.HunterCallThePackRuntime;
import com.feel.gems.power.ability.sentinel.SentinelTauntRuntime;
import com.feel.gems.power.bonus.BonusPassiveRuntime;
import com.feel.gems.power.gem.astra.SoulSystem;
import com.feel.gems.power.gem.beacon.BeaconAuraRuntime;
import com.feel.gems.power.gem.beacon.BeaconSupportRuntime;
import com.feel.gems.power.gem.chaos.ChaosSlotRuntime;
import com.feel.gems.power.gem.fire.AutoSmeltCache;
import com.feel.gems.power.gem.flux.FluxCharge;
import com.feel.gems.power.gem.hunter.HunterPreyMarkRuntime;
import com.feel.gems.power.gem.pillager.PillagerDiscipline;
import com.feel.gems.power.gem.pillager.PillagerVolleyRuntime;
import com.feel.gems.power.gem.puff.BreezyBashTracker;
import com.feel.gems.power.gem.space.SpaceAnomalies;
import com.feel.gems.power.gem.speed.SpeedFrictionlessSteps;
import com.feel.gems.power.gem.terror.TerrorRigRuntime;
import com.feel.gems.power.gem.spy.SpySystem;
import com.feel.gems.power.gem.summoner.SummonerCommanderMark;
import com.feel.gems.power.gem.summoner.SummonerSummons;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityRuntime;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.synergy.SynergyRuntime;
import com.feel.gems.stats.GemsStats;
import com.feel.gems.trust.GemTrust;
import com.feel.gems.util.GemsTickScheduler;
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
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
    private GemsModEvents() {
    }

    public static void register() {
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killedEntity, damageSource) -> {
            if (entity instanceof ServerPlayerEntity player) {
                LivingEntity living = killedEntity;
                SoulSystem.onKilledMob(player, living);
                SpySystem.recordLastKilledMob(player, living);

                if (!(killedEntity instanceof ServerPlayerEntity)) {
                    GemsStats.recordMobKill(player);
                }

                // Bonus passives: on kill effects (Bloodthirst, Adrenaline Rush)
                BonusPassiveRuntime.onKill(player, living);

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
                SpySystem.restoreStolenOnKill(killer, victim);
                com.feel.gems.item.legendary.HuntersTrophyNecklaceItem.restoreStolenOnKill(killer, victim);
                // Track last killer for Nemesis passive
                BonusPassiveRuntime.setLastKiller(victim, killer.getUuid());
                // Rivalry: handle target kill
                if (GemsBalance.v().rivalry().enabled()) {
                    RivalryManager.onPlayerKill(killer, victim);
                }
            }
        });

        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            if (sender instanceof ServerPlayerEntity player) {
                // Skinshift: the *target* cannot chat while any other player is impersonating them.
                if (SpySystem.isSkinshiftTarget(player)) {
                    player.sendMessage(Text.translatable("gems.spy.cannot_chat_skinshifted"), true);
                    return false;
                }
                Text disguise = SpySystem.chatDisguiseName(player);
                if (disguise != null) {
                    ServerPlayerEntity target = null;
                    java.util.UUID targetId = SpySystem.chatDisguiseTargetId(player);
                    if (targetId != null && player.getEntityWorld().getServer() != null) {
                        target = player.getEntityWorld().getServer().getPlayerManager().getPlayer(targetId);
                    }
                    Text displayName = target != null ? TitleDisplay.withTitlePrefix(target, disguise) : disguise;
                    net.minecraft.util.Formatting color = target != null ? TitleDisplay.titleColor(target) : null;
                    broadcastChatWithName(player, displayName, message.getContent(), color);
                    return false;
                }
                if (com.feel.gems.power.ability.duelist.DuelistMirrorMatchAbility.isInDuel(player)) {
                    String partnerStr = com.feel.gems.state.PlayerStateManager.getPersistent(
                            player, com.feel.gems.power.ability.duelist.DuelistMirrorMatchAbility.DUEL_PARTNER_KEY);
                    if (partnerStr != null && !partnerStr.isEmpty() && player.getEntityWorld().getServer() != null) {
                        try {
                            java.util.UUID partner = java.util.UUID.fromString(partnerStr);
                            ServerPlayerEntity partnerPlayer = player.getEntityWorld().getServer().getPlayerManager().getPlayer(partner);
                            if (partnerPlayer != null) {
                                broadcastChatWithName(player, Text.literal(partnerPlayer.getGameProfile().name()), message.getContent(), null);
                                return false;
                            }
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
                if (TitleDisplay.titlePrefix(player) != null) {
                    net.minecraft.util.Formatting color = TitleDisplay.titleColor(player);
                    broadcastChatWithName(player, TitleDisplay.withTitlePrefix(player, player.getName()), message.getContent(), color);
                    return false;
                }
            }
            return true;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) {
                return ActionResult.PASS;
            }
            if (!(player instanceof ServerPlayerEntity sp)) {
                return ActionResult.PASS;
            }
            if (!(entity instanceof LivingEntity living)) {
                return ActionResult.PASS;
            }

            java.util.UUID taunter = SentinelTauntRuntime.getTaunter(sp);
            if (taunter != null) {
                if (!(entity instanceof ServerPlayerEntity target && taunter.equals(target.getUuid()))) {
                    return ActionResult.FAIL;
                }
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
                    victim.stopUsingItem();
                    int cooldown = breakBuff
                            ? GemsBalance.v().pillager().vindicatorBreakShieldDisableCooldownTicks()
                            : GemsBalance.v().pillager().shieldbreakerDisableCooldownTicks();
                    if (cooldown > 0) {
                        victim.getItemCooldownManager().set(Items.SHIELD.getDefaultStack(), cooldown);
                    }
                }
            }

            // Hunter: Prey Mark - mark hit players
            if (living instanceof ServerPlayerEntity victim) {
                HunterPreyMarkRuntime.applyMark(sp, victim);
            }
            return ActionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient()) {
                return ActionResult.PASS;
            }
            if (!(player instanceof ServerPlayerEntity sp)) {
                return ActionResult.PASS;
            }
            if (world.getBlockState(hitResult.getBlockPos()).getBlock() instanceof net.minecraft.block.EnderChestBlock) {
                GemOwnership.purgeInventory(sp.getEntityWorld().getServer(), sp.getEnderChestInventory());
            }
            var blockEntity = world.getBlockEntity(hitResult.getBlockPos());
            if (blockEntity instanceof net.minecraft.inventory.Inventory inv) {
                GemOwnership.purgeInventoryIfPending(world.getServer(), inv);
            }
            if (com.feel.gems.power.gem.terror.TerrorRemoteChargeRuntime.tryArm(sp, hitResult.getBlockPos())) {
                com.feel.gems.power.runtime.AbilityFeedback.sound(sp, net.minecraft.sound.SoundEvents.ENTITY_TNT_PRIMED, 0.8F, 1.2F);
                sp.sendMessage(net.minecraft.text.Text.translatable("gems.terror.remote_charge_armed"), true);
                return ActionResult.SUCCESS;
            }
            if (TerrorRigRuntime.tryTriggerUse(sp, hitResult.getBlockPos())) {
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (world.isClient()) {
                return true;
            }
            if (player instanceof ServerPlayerEntity sp) {
                TerrorRigRuntime.tryTriggerBreak(sp, pos);
            }
            return true;
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            var persistent = ((com.feel.gems.state.GemsPersistentDataHolder) player).gems$getPersistentData();
            boolean grantStarterGem = persistent.getString("activeGem").isEmpty();
            GemPlayerState.initIfNeeded(player);
            AssassinState.initIfNeeded(player);
            if (AssassinState.maybeUnlockChoice(player) && AssassinState.isAssassin(player)) {
                AssassinState.sendChoicePrompt(player);
            }
            DuelistMirrorMatchRuntime.applyLogoutPenalty(player);
            GemPlayerState.applyMaxHearts(player);
            GemOwnership.consumeOfflinePenalty(player);
            GemKeepOnDeath.restore(player);
            if (grantStarterGem) {
                ensureActiveGemItem(player);
            }
            GemPowers.sync(player);
            GemItemGlint.sync(player);
            GemStateSync.send(player);
            sendDisables(player);
            SpySystem.syncSkinshifts(player);
            SpySystem.syncSkinshiftSelf(player);
            com.feel.gems.power.ability.trickster.TricksterControlSync.sync(player);
            unlockStartingRecipes(server, player);
            AssassinTeams.sync(server, player);
            LegendaryCrafting.deliverPending(player);
            RecallRelicItem.ensureForceload(player);
            HypnoControl.releaseAll(player);
            BountyBoard.notifyOnLogin(player);

            if (AssassinState.isEliminated(player)) {
                player.changeGameMode(net.minecraft.world.GameMode.SPECTATOR);
                player.sendMessage(net.minecraft.text.Text.translatable("gems.assassin.eliminated").formatted(net.minecraft.util.Formatting.RED), false);
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            AbilityRuntime.cleanupOnDisconnect(server, player);
            GemTrust.clearRuntimeCache(player.getUuid());
            LegendaryCooldowns.clearCache(player.getUuid());
            SummonerSummons.discardAll(player);
            PillagerVolleyRuntime.stop(player);
            HypnoControl.releaseAll(player);
            HunterCallThePackRuntime.onPlayerDisconnect(player.getUuid(), server);
            LegendaryDuels.onDisconnect(server, player);
            DuelistMirrorMatchRuntime.onDisconnect(player, server);
        });

        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            GemPlayerState.copy(oldPlayer, newPlayer);
            GemPlayerState.initIfNeeded(newPlayer);
            AssassinState.initIfNeeded(newPlayer);
            AssassinState.maybeUnlockChoice(newPlayer);
            if (!alive) {
            }
            GemPlayerState.applyMaxHearts(newPlayer);
            GemKeepOnDeath.restore(newPlayer);
            GemPowers.sync(newPlayer);
            GemItemGlint.sync(newPlayer);
            GemStateSync.send(newPlayer);
            SpySystem.syncSkinshifts(newPlayer);
            SpySystem.syncSkinshiftSelf(newPlayer);
            unlockStartingRecipes(newPlayer.getEntityWorld().getServer(), newPlayer);
            AssassinTeams.sync(newPlayer.getEntityWorld().getServer(), newPlayer);
            LegendaryCrafting.deliverPending(newPlayer);
            RecallRelicItem.ensureForceload(newPlayer);
            HypnoControl.releaseAll(newPlayer);

            if (AssassinState.isEliminated(newPlayer)) {
                newPlayer.changeGameMode(net.minecraft.world.GameMode.SPECTATOR);
                newPlayer.sendMessage(net.minecraft.text.Text.translatable("gems.assassin.eliminated").formatted(net.minecraft.util.Formatting.RED), false);
            }
            LegendaryDuels.onPlayerCopyFrom(oldPlayer, newPlayer, alive);

            // Rivalry: assign a new target on spawn/respawn
            if (GemsBalance.v().rivalry().enabled()) {
                RivalryManager.assignTarget(newPlayer);
            }
        });

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> AutoSmeltCache.clear());

        ServerTickEvents.START_SERVER_TICK.register(server -> GemsPerfMonitor.onTickStart());

        GemsTickScheduler.register();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            GemsTickScheduler.clearAll();
            SynergyRuntime.clearAll();

            // Every tick: lightweight runtimes + maintenance queues.
            GemsTickScheduler.scheduleRepeating(server, 0, 1, s -> {
                com.feel.gems.state.PlayerStateManager.tickAll();
                BreezyBashTracker.tick(s);
                SpaceAnomalies.tick(s);
                PillagerVolleyRuntime.tick(s);
                SpeedFrictionlessSteps.tick(s);
                GemsStressTest.tick(s);
                GemOwnership.tickPurgeQueue(s);
                ChaosSlotRuntime.tick(s);
                HunterCallThePackRuntime.tick(s);

                // Clean up old synergy casts (every 20 ticks)
                if (s.getTicks() % 20 == 0) {
                    SynergyRuntime.cleanup(s.getOverworld().getTime());
                    GemOwnership.purgePendingPlayerInventories(s);
                }

                // Hunting Trap: check mob triggers (players are handled via per-player checkStep).
                if (!com.feel.gems.power.ability.hunter.HunterHuntingTrapAbility.ACTIVE_TRAPS.isEmpty()) {
                    for (ServerWorld world : s.getWorlds()) {
                        com.feel.gems.power.ability.hunter.HunterHuntingTrapAbility.tickWorld(world);
                    }
                }
                // Sentinel ability runtime ticks (skip world scans if nothing is active)
                if (com.feel.gems.power.ability.sentinel.SentinelShieldWallRuntime.hasAnyWalls()
                        || com.feel.gems.power.ability.sentinel.SentinelLockdownRuntime.hasAnyZones()) {
                    for (ServerWorld world : s.getWorlds()) {
                        long currentTime = world.getTime();
                        String worldId = world.getRegistryKey().getValue().toString();
                        com.feel.gems.power.ability.sentinel.SentinelShieldWallRuntime.tick(currentTime, worldId, world);
                        com.feel.gems.power.ability.sentinel.SentinelLockdownRuntime.tick(currentTime, worldId, world);
                    }
                }
                for (ServerWorld world : s.getWorlds()) {
                    com.feel.gems.power.ability.bonus.BonusVoidRiftAbility.tick(world);
                    com.feel.gems.power.ability.bonus.BonusStormCallAbility.tick(world);
                    com.feel.gems.power.ability.bonus.BonusPhantasmAbility.tick(world);
                }
                for (ServerPlayerEntity player : s.getPlayerManager().getPlayerList()) {
                    TerrorRigRuntime.checkStep(player);
                    com.feel.gems.power.ability.hunter.HunterHuntingTrapAbility.checkStep(player);
                    com.feel.gems.power.ability.duelist.DuelistMirrorMatchRuntime.tick(player);
                    com.feel.gems.power.ability.trickster.TricksterPuppetRuntime.tickPuppetedPlayer(player);
                    com.feel.gems.power.ability.trickster.TricksterMirageRuntime.tickMirage(player);
                    // Trickster mirage particles (every 4 ticks for visual effect without heavy load)
                    if (player.getEntityWorld().getTime() % 4 == 0) {
                        com.feel.gems.power.ability.trickster.TricksterMirageRuntime.tickMirageParticles(player);
                    }
                    if (s.getTicks() % 20 == 0 && LegendaryCooldowns.updateCharmCount(player)) {
                        GemCooldownSync.send(player);
                    }
                }
            });

            // Every second: per-player runtimes and periodic server systems.
            GemsTickScheduler.scheduleRepeating(server, 20, 20, s -> {
                LegendaryCrafting.tick(s);
                LegendaryPlayerTracker.tick(s);
                com.feel.gems.legendary.GemSeerTracker.tick(s);
                TerrorRigRuntime.tick(s);
                for (ServerWorld world : s.getWorlds()) {
                    com.feel.gems.power.ability.hunter.HunterHuntingTrapAbility.cleanExpiredTraps(world);
                }
                LegendaryDuels.tickEverySecond(s);

                for (ServerPlayerEntity player : s.getPlayerManager().getPlayerList()) {
                    GemPlayerState.initIfNeeded(player);
                    if (GemPlayerState.getActiveGem(player) == GemId.FLUX && GemPlayerState.getEnergy(player) > 0) {
                        FluxCharge.tickOvercharge(player);
                    }
                    AbilityRuntime.tickEverySecond(player);
                    PillagerDiscipline.tick(player);
                    SpySystem.tickEverySecond(player);
                    com.feel.gems.power.ability.trickster.TricksterControlSync.sync(player);
                    BeaconSupportRuntime.tickEverySecond(player);
                    BeaconAuraRuntime.tickEverySecond(player);
                    com.feel.gems.power.gem.speed.SpeedAutoStepRuntime.tickEverySecond(player);
                    com.feel.gems.power.gem.hunter.HunterPreyMarkRuntime.tickTrackersEye(player);
                    AugmentRuntime.applyLegendaryModifiers(player);
                    // Mastery aura particles
                    MasteryAuraRuntime.tick(player);
                    // Bonus passives tick handler
                    if (player.getEntityWorld() instanceof ServerWorld world) {
                        BonusPassiveRuntime.tickEverySecond(player, world);
                        BonusPassiveRuntime.tickCombatMeditate(player);
                        BonusPassiveRuntime.tickSixthSense(player, world);
                        BonusPassiveRuntime.tickMagneticPull(player, world);
                    }
                    // Sync bonus abilities state (for HUD cooldown updates)
                    if (GemPlayerState.getEnergy(player) >= 10) {
                        GemStateSync.sendBonusAbilitiesSync(player);
                    }
                }
            });

            // Every two seconds: heavier follow/cleanup work.
            GemsTickScheduler.scheduleRepeating(server, 40, 40, s -> {
                for (ServerPlayerEntity player : s.getPlayerManager().getPlayerList()) {
                    GemPowers.maintain(player);
                    SupremeSetRuntime.tick(player);
                    HypnoControl.pruneAndCount(player);
                    com.feel.gems.power.gem.astra.SoulSummons.pruneAndCount(player);
                    HypnoControl.followOwner(player);
                    com.feel.gems.power.gem.astra.SoulSummons.followOwner(player);
                    SummonerSummons.followOwner(player);
                    RecallRelicItem.clearIfMissingItem(player);
                }
            });

            // Every minute: leaderboard updates for general titles.
            GemsTickScheduler.scheduleRepeating(server, 1200, 1200, s -> {
                com.feel.gems.mastery.LeaderboardTracker.updateLeaderboards(s);
            });

            // Tail hook: perf monitor should run after all scheduled work for the tick.
            GemsTickScheduler.scheduleRepeating(server, 0, 1, Integer.MAX_VALUE, s -> GemsPerfMonitor.onTickEnd());
        });
    }

    private static void sendDisables(ServerPlayerEntity player) {
        var v = GemsDisables.v();
        if (player != null && player.getCommandSource().getPermissions().hasPermission(new net.minecraft.command.permission.Permission.Level(net.minecraft.command.permission.PermissionLevel.fromLevel(2)))) {
            ServerPlayNetworking.send(player, new ServerDisablesPayload(java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of()));
            return;
        }
        java.util.List<Integer> disabledGems = v.disabledGems().stream().map(com.feel.gems.core.GemId::ordinal).sorted().toList();
        java.util.List<String> disabledAbilities = v.disabledAbilities().stream().map(net.minecraft.util.Identifier::toString).sorted().toList();
        java.util.List<String> disabledPassives = v.disabledPassives().stream().map(net.minecraft.util.Identifier::toString).sorted().toList();
        java.util.List<String> disabledBonusAbilities = v.disabledBonusAbilities().stream().map(net.minecraft.util.Identifier::toString).sorted().toList();
        java.util.List<String> disabledBonusPassives = v.disabledBonusPassives().stream().map(net.minecraft.util.Identifier::toString).sorted().toList();
        ServerPlayNetworking.send(player, new ServerDisablesPayload(disabledGems, disabledAbilities, disabledPassives, disabledBonusAbilities, disabledBonusPassives));
    }

    private static void ensureActiveGemItem(ServerPlayerEntity player) {
        Item gemItem = ModItems.gemItem(GemPlayerState.getActiveGem(player));
        if (hasItem(player, gemItem)) {
            return;
        }
        ItemStack stack = new ItemStack(gemItem);
        com.feel.gems.item.GemOwnership.tagOwned(stack, player);
        player.giveItemStack(stack);
    }

    private static boolean hasItem(ServerPlayerEntity player, Item item) {
        for (ItemStack stack : player.getInventory().getMainStacks()) {
            if (stack.isOf(item)) {
                return true;
            }
        }
        if (player.getOffHandStack().isOf(item)) return true;
        if (player.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD).isOf(item)) return true;
        if (player.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST).isOf(item)) return true;
        if (player.getEquippedStack(net.minecraft.entity.EquipmentSlot.LEGS).isOf(item)) return true;
        if (player.getEquippedStack(net.minecraft.entity.EquipmentSlot.FEET).isOf(item)) return true;
        return false;
    }

    private static void broadcastChatWithName(ServerPlayerEntity sender, Text name, Text content, net.minecraft.util.Formatting color) {
        if (sender == null || sender.getEntityWorld().getServer() == null) {
            return;
        }
        Text styledContent = content;
        if (color != null) {
            styledContent = content.copy().setStyle(content.getStyle().withColor(color));
        }
        Text message = Text.translatable("chat.type.text", name, styledContent);
        for (ServerPlayerEntity player : sender.getEntityWorld().getServer().getPlayerManager().getPlayerList()) {
            player.sendMessage(message, false);
        }
    }

    private static void purgeOwnedGems(ServerPlayerEntity player) {
        if (player.getEntityWorld().getServer() == null) {
            return;
        }
        com.feel.gems.item.GemOwnership.requestDeferredPurge(player.getEntityWorld().getServer(), player.getUuid());
    }

    public static void unlockStartingRecipes(net.minecraft.server.MinecraftServer server, ServerPlayerEntity player) {
        if (server == null) {
            return;
        }
        var manager = server.getRecipeManager();
        List<RecipeEntry<?>> entries = new ArrayList<>();
        for (RecipeEntry<?> entry : manager.values()) {
            if (GemsMod.MOD_ID.equals(entry.id().getValue().getNamespace())) {
                entries.add(entry);
            }
        }
        if (!entries.isEmpty()) {
            player.unlockRecipes(entries);
        }
    }
}
