package com.feel.gems.command;

import com.feel.gems.admin.GemsAdmin;
import com.feel.gems.augment.AugmentDefinition;
import com.feel.gems.augment.AugmentInstance;
import com.feel.gems.augment.AugmentRegistry;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.augment.AugmentRarity;
import com.feel.gems.assassin.AssassinState;
import com.feel.gems.assassin.AssassinTeams;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemDefinition;
import com.feel.gems.core.GemEnergyState;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.debug.GemsPerfMonitor;
import com.feel.gems.debug.GemsStressTest;
import com.feel.gems.item.GemItemGlint;
import com.feel.gems.item.ModItems;
import com.feel.gems.item.legendary.TrackerCompassItem;
import com.feel.gems.mastery.GemMastery;
import com.feel.gems.mastery.MasteryReward;
import com.feel.gems.mastery.MasteryRewards;
import com.feel.gems.net.GemStateSync;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.gem.astra.SoulSystem;
import com.feel.gems.power.gem.flux.FluxCharge;
import com.feel.gems.power.registry.ModAbilities;
import com.feel.gems.power.registry.ModPassives;
import com.feel.gems.power.runtime.GemAbilities;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.stats.GemsStats;
import com.feel.gems.trade.GemTrading;
import com.feel.gems.trust.GemTrust;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;




public final class GemsCommands {
    private GemsCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("gems")
                        .then(CommandManager.literal("status")
                                .executes(ctx -> status(ctx.getSource(), ctx.getSource().getPlayerOrThrow())))
                        .then(CommandManager.literal("reloadBalance")
                                .requires(src -> src.getPermissions().hasPermission(new Permission.Level(PermissionLevel.fromLevel(2))))
                                .executes(ctx -> reloadBalance(ctx.getSource())))
                        .then(CommandManager.literal("dumpBalance")
                                .requires(src -> src.getPermissions().hasPermission(new Permission.Level(PermissionLevel.fromLevel(2))))
                                .executes(ctx -> dumpBalance(ctx.getSource())))
                        .then(CommandManager.literal("trust")
                                .then(CommandManager.argument("players", EntityArgumentType.players())
                                        .executes(ctx -> {
                                            ServerPlayerEntity owner = ctx.getSource().getPlayerOrThrow();
                                            for (ServerPlayerEntity other : EntityArgumentType.getPlayers(ctx, "players")) {
                                                trust(owner, other);
                                            }
                                            return 1;
                                        })))
                        .then(CommandManager.literal("untrust")
                                .then(CommandManager.argument("players", EntityArgumentType.players())
                                        .executes(ctx -> {
                                            ServerPlayerEntity owner = ctx.getSource().getPlayerOrThrow();
                                            for (ServerPlayerEntity other : EntityArgumentType.getPlayers(ctx, "players")) {
                                                untrust(owner, other);
                                            }
                                            return 1;
                                        })))
                        .then(CommandManager.literal("trustlist")
                                .executes(ctx -> trustList(ctx.getSource().getPlayerOrThrow())))
                        .then(CommandManager.literal("track")
                                .then(CommandManager.argument("player", StringArgumentType.word())
                                        .suggests((ctx, builder) -> suggestOnlinePlayers(ctx.getSource(), builder))
                                        .executes(ctx -> track(ctx.getSource().getPlayerOrThrow(), StringArgumentType.getString(ctx, "player")))))
                        .then(CommandManager.literal("trade")
                                .then(CommandManager.argument("gem", StringArgumentType.word())
                                        .suggests((ctx, builder) -> suggestGems(builder))
                                        .executes(ctx -> trade(ctx.getSource().getPlayerOrThrow(), StringArgumentType.getString(ctx, "gem")))))
                        .then(CommandManager.literal("admin")
                                .requires(src -> src.getPermissions().hasPermission(new Permission.Level(PermissionLevel.fromLevel(2))))
                                .then(CommandManager.literal("status")
                                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                                .executes(ctx -> {
                                                    for (ServerPlayerEntity player : EntityArgumentType.getPlayers(ctx, "players")) {
                                                        status(ctx.getSource(), player);
                                                    }
                                                    return 1;
                                                })))
                                .then(CommandManager.literal("cast")
                                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                                .then(CommandManager.argument("slot", IntegerArgumentType.integer(1, 10))
                                                        .executes(ctx -> {
                                                            int slot = IntegerArgumentType.getInteger(ctx, "slot");
                                                            for (ServerPlayerEntity player : EntityArgumentType.getPlayers(ctx, "players")) {
                                                                castSlot(ctx.getSource(), player, slot);
                                                            }
                                                            return 1;
                                                        }))))
                                .then(CommandManager.literal("resync")
                                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                                .executes(ctx -> {
                                                    for (ServerPlayerEntity player : EntityArgumentType.getPlayers(ctx, "players")) {
                                                        GemPlayerState.initIfNeeded(player);
                                                        resync(player);
                                                        ctx.getSource().sendFeedback(() -> Text.literal("Resynced " + player.getName().getString()), true);
                                                    }
                                                    return 1;
                                                })))
                                .then(CommandManager.literal("giveItem")
                                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                                .then(CommandManager.argument("item", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> suggestAdminItems(builder))
                                                        .executes(ctx -> {
                                                            String item = StringArgumentType.getString(ctx, "item");
                                                            for (ServerPlayerEntity player : EntityArgumentType.getPlayers(ctx, "players")) {
                                                                giveItem(ctx.getSource(), player, item);
                                                            }
                                                            return 1;
                                                        }))))
                                .then(CommandManager.literal("giveGem")
                                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                                .then(CommandManager.argument("gem", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> suggestGems(builder))
                                                        .executes(ctx -> {
                                                            String gem = StringArgumentType.getString(ctx, "gem");
                                                            for (ServerPlayerEntity player : EntityArgumentType.getPlayers(ctx, "players")) {
                                                                giveGem(ctx.getSource(), player, gem);
                                                            }
                                                            return 1;
                                                        }))))
                                .then(CommandManager.literal("giveAllGems")
                                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                                .executes(ctx -> giveAllGems(ctx.getSource(), EntityArgumentType.getPlayers(ctx, "players")))))
                                .then(CommandManager.literal("giveAllLegendaries")
                                    .then(CommandManager.argument("players", EntityArgumentType.players())
                                        .executes(ctx -> giveAllLegendaries(ctx.getSource(), EntityArgumentType.getPlayers(ctx, "players")))))
                                .then(CommandManager.literal("clearGems")
                                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                                .executes(ctx -> clearGems(ctx.getSource(), EntityArgumentType.getPlayers(ctx, "players")))))
                                .then(CommandManager.literal("setEnergy")
                                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                                .then(CommandManager.argument("energy", IntegerArgumentType.integer(GemPlayerState.MIN_ENERGY, GemPlayerState.MAX_ENERGY))
                                                        .executes(ctx -> {
                                                            int energy = IntegerArgumentType.getInteger(ctx, "energy");
                                                            for (ServerPlayerEntity player : EntityArgumentType.getPlayers(ctx, "players")) {
                                                                setEnergy(ctx.getSource(), player, energy);
                                                            }
                                                            return 1;
                                                        }))))
                                .then(CommandManager.literal("setHearts")
                                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                                .then(CommandManager.argument("hearts", IntegerArgumentType.integer(GemPlayerState.minMaxHearts(), GemPlayerState.MAX_MAX_HEARTS))
                                                        .executes(ctx -> {
                                                            int hearts = IntegerArgumentType.getInteger(ctx, "hearts");
                                                            for (ServerPlayerEntity player : EntityArgumentType.getPlayers(ctx, "players")) {
                                                                setHearts(ctx.getSource(), player, hearts);
                                                            }
                                                            return 1;
                                                        }))))
                                .then(CommandManager.literal("setGem")
                                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                                .then(CommandManager.argument("gem", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> suggestGems(builder))
                                                        .executes(ctx -> {
                                                            String gem = StringArgumentType.getString(ctx, "gem");
                                                            for (ServerPlayerEntity player : EntityArgumentType.getPlayers(ctx, "players")) {
                                                                setGem(ctx.getSource(), player, gem);
                                                            }
                                                            return 1;
                                                        }))))
                                .then(CommandManager.literal("reset")
                                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                                .executes(ctx -> {
                                                    for (ServerPlayerEntity player : EntityArgumentType.getPlayers(ctx, "players")) {
                                                        reset(ctx.getSource(), player);
                                                    }
                                                    return 1;
                                                })))
                                .then(CommandManager.literal("cooldowns")
                                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                                .then(CommandManager.argument("disabled", BoolArgumentType.bool())
                                                        .executes(ctx -> setCooldownsDisabled(
                                                                ctx.getSource(),
                                                                EntityArgumentType.getPlayers(ctx, "players"),
                                                                BoolArgumentType.getBool(ctx, "disabled")
                                                        )))))
                                .then(CommandManager.literal("legendaryCooldowns")
                                    .then(CommandManager.argument("players", EntityArgumentType.players())
                                        .then(CommandManager.argument("disabled", BoolArgumentType.bool())
                                            .executes(ctx -> setLegendaryCooldownsDisabled(
                                                ctx.getSource(),
                                                EntityArgumentType.getPlayers(ctx, "players"),
                                                BoolArgumentType.getBool(ctx, "disabled")
                                            )))))
                                .then(CommandManager.literal("stats")
                                    .then(CommandManager.literal("show")
                                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                            .executes(ctx -> statsShow(
                                                ctx.getSource(),
                                                EntityArgumentType.getPlayers(ctx, "players")
                                            ))))
                                    .then(CommandManager.literal("set")
                                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                            .then(CommandManager.argument("stat", StringArgumentType.word())
                                                .then(CommandManager.argument("value", IntegerArgumentType.integer(0, 1000000))
                                                    .executes(ctx -> statsSet(
                                                        ctx.getSource(),
                                                        EntityArgumentType.getPlayers(ctx, "players"),
                                                        StringArgumentType.getString(ctx, "stat"),
                                                        IntegerArgumentType.getInteger(ctx, "value")
                                                    ))))))
                                    .then(CommandManager.literal("reset")
                                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                            .executes(ctx -> statsReset(
                                                ctx.getSource(),
                                                EntityArgumentType.getPlayers(ctx, "players")
                                            )))))
                                .then(CommandManager.literal("assassin")
                                    .then(CommandManager.literal("set")
                                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                            .then(CommandManager.argument("value", BoolArgumentType.bool())
                                                .executes(ctx -> setAssassin(
                                                    ctx.getSource(),
                                                    EntityArgumentType.getPlayers(ctx, "players"),
                                                    BoolArgumentType.getBool(ctx, "value")
                                                )))))
                                    .then(CommandManager.literal("setHearts")
                                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                            .then(CommandManager.argument("hearts", IntegerArgumentType.integer(0, AssassinState.maxHearts()))
                                                .executes(ctx -> setAssassinHearts(
                                                    ctx.getSource(),
                                                    EntityArgumentType.getPlayers(ctx, "players"),
                                                    IntegerArgumentType.getInteger(ctx, "hearts")
                                                )))))
                                    .then(CommandManager.literal("setCounter")
                                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                            .then(CommandManager.argument("counter", StringArgumentType.word())
                                                .then(CommandManager.argument("value", IntegerArgumentType.integer(0, 1000000))
                                                    .executes(ctx -> setAssassinCounter(
                                                        ctx.getSource(),
                                                        EntityArgumentType.getPlayers(ctx, "players"),
                                                        StringArgumentType.getString(ctx, "counter"),
                                                        IntegerArgumentType.getInteger(ctx, "value")
                                                    ))))))
                                    .then(CommandManager.literal("reset")
                                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                            .executes(ctx -> resetAssassin(ctx.getSource(), EntityArgumentType.getPlayers(ctx, "players"))))))
                                .then(CommandManager.literal("enhancements")
                                    .then(CommandManager.literal("setCapPenalty")
                                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                            .then(CommandManager.argument("value", IntegerArgumentType.integer(0, GemPlayerState.MAX_ENERGY))
                                                .executes(ctx -> setEnhancementPenalty(
                                                    ctx.getSource(),
                                                    EntityArgumentType.getPlayers(ctx, "players"),
                                                    IntegerArgumentType.getInteger(ctx, "value")
                                                ))))))
                                .then(CommandManager.literal("stats")
                                    .then(CommandManager.literal("show")
                                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                            .executes(ctx -> statsShow(
                                                ctx.getSource(),
                                                EntityArgumentType.getPlayers(ctx, "players")
                                            ))))
                                    .then(CommandManager.literal("set")
                                    .then(CommandManager.argument("players", EntityArgumentType.players())
                                        .then(CommandManager.argument("stat", StringArgumentType.word())
                                        .then(CommandManager.argument("value", IntegerArgumentType.integer(0, 1000000))
                                            .executes(ctx -> statsSet(
                                            ctx.getSource(),
                                            EntityArgumentType.getPlayers(ctx, "players"),
                                            StringArgumentType.getString(ctx, "stat"),
                                            IntegerArgumentType.getInteger(ctx, "value")
                                            ))))))
                                    .then(CommandManager.literal("reset")
                                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                            .executes(ctx -> statsReset(
                                                ctx.getSource(),
                                                EntityArgumentType.getPlayers(ctx, "players")
                                            )))))
                                .then(CommandManager.literal("assassin")
                                    .then(CommandManager.literal("set")
                                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                            .then(CommandManager.argument("value", BoolArgumentType.bool())
                                                .executes(ctx -> setAssassin(
                                                    ctx.getSource(),
                                                    EntityArgumentType.getPlayers(ctx, "players"),
                                                    BoolArgumentType.getBool(ctx, "value")
                                                )))))
                                    .then(CommandManager.literal("setHearts")
                                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                            .then(CommandManager.argument("hearts", IntegerArgumentType.integer(0, AssassinState.maxHearts()))
                                                .executes(ctx -> setAssassinHearts(
                                                    ctx.getSource(),
                                                    EntityArgumentType.getPlayers(ctx, "players"),
                                                    IntegerArgumentType.getInteger(ctx, "hearts")
                                                )))))
                                    .then(CommandManager.literal("setCounter")
                                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                            .then(CommandManager.argument("counter", StringArgumentType.word())
                                                .then(CommandManager.argument("value", IntegerArgumentType.integer(0, 1000000))
                                                    .executes(ctx -> setAssassinCounter(
                                                        ctx.getSource(),
                                                        EntityArgumentType.getPlayers(ctx, "players"),
                                                        StringArgumentType.getString(ctx, "counter"),
                                                        IntegerArgumentType.getInteger(ctx, "value")
                                                    ))))))
                                        .then(CommandManager.literal("reset")
                                                .then(CommandManager.argument("players", EntityArgumentType.players())
                                                        .executes(ctx -> resetAssassin(ctx.getSource(), EntityArgumentType.getPlayers(ctx, "players"))))))
                                .then(CommandManager.literal("enhancements")
                                    .then(CommandManager.literal("setCapPenalty")
                                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                            .then(CommandManager.argument("value", IntegerArgumentType.integer(0, GemPlayerState.MAX_ENERGY))
                                                .executes(ctx -> setEnhancementPenalty(
                                                    ctx.getSource(),
                                                    EntityArgumentType.getPlayers(ctx, "players"),
                                                    IntegerArgumentType.getInteger(ctx, "value")
                                                ))))))
                                .then(CommandManager.literal("perf")
                                        .then(CommandManager.literal("reset")
                                                .executes(ctx -> perfReset(ctx.getSource())))
                                        .then(CommandManager.literal("snapshot")
                                                .executes(ctx -> perfSnapshot(ctx.getSource(), 1200))
                                                .then(CommandManager.argument("windowTicks", IntegerArgumentType.integer(1, 72000))
                                                        .executes(ctx -> perfSnapshot(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "windowTicks"))))))
                                .then(CommandManager.literal("stress")
                                        .then(CommandManager.literal("start")
                                                .then(CommandManager.argument("players", EntityArgumentType.players())
                                                        .then(CommandManager.argument("seconds", IntegerArgumentType.integer(1, 3600))
                                                                .then(CommandManager.argument("periodTicks", IntegerArgumentType.integer(1, 200))
                                                                        .then(CommandManager.argument("mode", StringArgumentType.word())
                                                                                .suggests((ctx, builder) -> suggestStressMode(builder))
                                                                                .then(CommandManager.argument("cycleGems", BoolArgumentType.bool())
                                                                                        .then(CommandManager.argument("forceEnergy10", BoolArgumentType.bool())
                                                                                                .executes(ctx -> stressStart(
                                                                                                        ctx.getSource(),
                                                                                                        EntityArgumentType.getPlayers(ctx, "players"),
                                                                                                        IntegerArgumentType.getInteger(ctx, "seconds"),
                                                                                                        IntegerArgumentType.getInteger(ctx, "periodTicks"),
                                                                                                        GemsStressTest.parseMode(StringArgumentType.getString(ctx, "mode")),
                                                                                                        BoolArgumentType.getBool(ctx, "cycleGems"),
                                                                                                        BoolArgumentType.getBool(ctx, "forceEnergy10")
                                                                                                ))))))))))
                                        .then(CommandManager.literal("stop")
                                                .then(CommandManager.argument("players", EntityArgumentType.players())
                                                        .executes(ctx -> stressStop(ctx.getSource(), EntityArgumentType.getPlayers(ctx, "players"))))))
                        );

                registerTitleCommands(dispatcher);
                registerAugmentCommands(dispatcher);
    }

    private static int status(ServerCommandSource source, ServerPlayerEntity player) {
        GemPlayerState.initIfNeeded(player);
        AssassinState.initIfNeeded(player);

        GemId active = GemPlayerState.getActiveGem(player);
        int energy = GemPlayerState.getEnergy(player);
        int hearts = GemPlayerState.getMaxHearts(player);
        boolean assassin = AssassinState.isAssassin(player);

        GemDefinition def = GemRegistry.definition(active);
        GemEnergyState energyState = new GemEnergyState(energy);

        List<Identifier> passives = def.availablePassives(energyState);
        List<Identifier> abilities = def.availableAbilities(energyState);

        source.sendFeedback(() -> Text.literal("Player: " + player.getName().getString()), false);
        source.sendFeedback(() -> Text.literal("Gem: " + active.name()), false);
        source.sendFeedback(() -> Text.literal("Energy: " + energyState.tier().name() + " (" + energy + "/10)"), false);
        if (assassin) {
            int maxAssassinHearts = AssassinState.maxHearts();
            source.sendFeedback(() -> Text.literal("Assassin: YES (hearts=" + AssassinState.getAssassinHearts(player) + "/" + maxAssassinHearts + ", eliminated=" + AssassinState.isEliminated(player) + ")"), false);
        } else {
            source.sendFeedback(() -> Text.literal("Assassin: no"), false);
        }
        source.sendFeedback(() -> Text.literal("Max hearts: " + hearts + "/20 (base system)"), false);
        source.sendFeedback(() -> Text.literal("Passives: " + (passives.isEmpty() ? "-" : passives.stream().map(GemsCommands::passiveName).reduce((a, b) -> a + ", " + b).orElse("-"))), false);
        source.sendFeedback(() -> Text.literal("Abilities: " + (abilities.isEmpty() ? "-" : abilities.stream().map(GemsCommands::abilityName).reduce((a, b) -> a + ", " + b).orElse("-"))), false);
        source.sendFeedback(() -> Text.literal("Owned: " + GemPlayerState.getOwnedGems(player)), false);
        source.sendFeedback(() -> Text.literal("Trusted: " + GemTrust.getTrusted(player).size()), false);
        source.sendFeedback(() -> Text.literal("Kills (total): normal=" + AssassinState.totalNormalKills(player) + " final=" + AssassinState.totalFinalKills(player) + " points=" + AssassinState.totalPoints(player)), false);
        if (assassin) {
            source.sendFeedback(() -> Text.literal("Kills (as assassin): normal=" + AssassinState.assassinNormalKills(player) + " final=" + AssassinState.assassinFinalKills(player) + " points=" + AssassinState.assassinPoints(player)), false);
            source.sendFeedback(() -> Text.literal("Winner points (vs non-assassins): normal=" + AssassinState.assassinNormalKillsVsNonAssassins(player) + " final=" + AssassinState.assassinFinalKillsVsNonAssassins(player) + " points=" + AssassinState.assassinPointsVsNonAssassins(player)), false);
        }
        if (active == GemId.FLUX) {
            source.sendFeedback(() -> Text.literal("Flux charge: " + FluxCharge.get(player) + "%"), false);
        }
        return 1;
    }

    private static int castSlot(ServerCommandSource source, ServerPlayerEntity player, int slotNumber) {
        GemPlayerState.initIfNeeded(player);
        GemId active = GemPlayerState.getActiveGem(player);
        int abilityCount = GemRegistry.definition(active).abilities().size();

        int zeroBased = slotNumber - 1;
        if (active == GemId.ASTRA && zeroBased == abilityCount) {
            SoulSystem.release(player);
            source.sendFeedback(() -> Text.literal("Cast Soul Release for " + player.getName().getString()), true);
            return 1;
        }
        if (zeroBased < 0 || zeroBased >= abilityCount) {
            source.sendError(Text.literal("Slot " + slotNumber + " is not valid for " + active.name() + " (abilities: " + abilityCount + ")."));
            return 0;
        }

        GemAbilities.activateByIndex(player, zeroBased);
        source.sendFeedback(() -> Text.literal("Cast slot " + slotNumber + " for " + player.getName().getString() + " (" + active.name() + ")"), true);
        return 1;
    }

    private static int perfReset(ServerCommandSource source) {
        GemsPerfMonitor.reset();
        source.sendFeedback(() -> Text.literal("GEMS_PERF reset"), true);
        return 1;
    }

    private static int perfSnapshot(ServerCommandSource source, int windowTicks) {
        GemsPerfMonitor.Snapshot snap = GemsPerfMonitor.snapshot(windowTicks);
        source.sendFeedback(() -> Text.literal(String.format(
                java.util.Locale.ROOT,
                "GEMS_PERF samples=%d avg_mspt=%.2f med_mspt=%.2f p95_mspt=%.2f max_mspt=%.2f",
                snap.samples(),
                snap.avgMspt(),
                snap.medianMspt(),
                snap.p95Mspt(),
                snap.maxMspt()
        )), false);
        return 1;
    }

    private static int reloadBalance(ServerCommandSource source) {
        GemsBalance.ReloadResult result = GemsBalance.reloadFromDisk();
        if (!result.applied()) {
            String error = result.loadResult().error();
            source.sendError(Text.literal(error == null ? "Failed to reload balance config." : error));
            return 0;
        }

        source.sendFeedback(() -> Text.literal("Reloaded balance config (" + result.loadResult().status() + "): " + result.loadResult().path()), true);
        return 1;
    }

    private static int dumpBalance(ServerCommandSource source) {
        var out = GemsBalance.dumpEffectiveBalance();
        source.sendFeedback(() -> Text.literal("Wrote effective balance to: " + out), true);
        return 1;
    }

    private static int setCooldownsDisabled(ServerCommandSource source, java.util.Collection<ServerPlayerEntity> players, boolean disabled) {
        for (ServerPlayerEntity player : players) {
            GemsAdmin.setNoCooldowns(player, disabled);
        }
        source.sendFeedback(() -> Text.literal((disabled ? "Disabled" : "Enabled") + " ability cooldowns for " + players.size() + " player(s)."), true);
        return 1;
    }

    private static int setLegendaryCooldownsDisabled(ServerCommandSource source, java.util.Collection<ServerPlayerEntity> players, boolean disabled) {
        for (ServerPlayerEntity player : players) {
            GemsAdmin.setNoLegendaryCooldowns(player, disabled);
        }
        source.sendFeedback(() -> Text.literal((disabled ? "Disabled" : "Enabled") + " legendary item cooldowns for " + players.size() + " player(s)."), true);
        return 1;
    }

    private static int statsShow(ServerCommandSource source, java.util.Collection<ServerPlayerEntity> players) {
        for (ServerPlayerEntity player : players) {
            List<Text> lines = GemsStats.buildSummary(player);
            for (Text line : lines) {
                source.sendFeedback(() -> line, false);
            }
        }
        return 1;
    }

    private static int statsReset(ServerCommandSource source, java.util.Collection<ServerPlayerEntity> players) {
        for (ServerPlayerEntity player : players) {
            GemsStats.reset(player);
            source.sendFeedback(() -> Text.literal("Reset stats for " + player.getName().getString()), true);
        }
        return 1;
    }

    private static int statsSet(ServerCommandSource source, java.util.Collection<ServerPlayerEntity> players, String stat, int value) {
        int updated = 0;
        for (ServerPlayerEntity player : players) {
            if (GemsStats.setStat(player, stat, value)) {
                updated++;
                source.sendFeedback(() -> Text.literal("Set stat " + stat + "=" + value + " for " + player.getName().getString()), true);
            } else {
                source.sendError(Text.literal("Unknown stat '" + stat + "'."));
            }
        }
        return updated > 0 ? 1 : 0;
    }

    private static int resetAssassin(ServerCommandSource source, java.util.Collection<ServerPlayerEntity> players) {
        for (ServerPlayerEntity player : players) {
            boolean wasEliminated = AssassinState.isEliminated(player);
            AssassinState.reset(player);
            if (wasEliminated && player.isSpectator()) {
                player.changeGameMode(GameMode.SURVIVAL);
            }
            GemPlayerState.initIfNeeded(player);
            resync(player);
            AssassinTeams.sync(source.getServer(), player);
            source.sendFeedback(() -> Text.literal("Reset assassin state for " + player.getName().getString()), true);
        }
        return 1;
    }

    private static int setAssassin(ServerCommandSource source, java.util.Collection<ServerPlayerEntity> players, boolean value) {
        for (ServerPlayerEntity player : players) {
            AssassinState.setAssassin(player, value);
            if (!value && player.isSpectator()) {
                player.changeGameMode(GameMode.SURVIVAL);
            }
            GemPlayerState.initIfNeeded(player);
            resync(player);
            AssassinTeams.sync(source.getServer(), player);
            source.sendFeedback(() -> Text.literal("Set assassin=" + value + " for " + player.getName().getString()), true);
        }
        return 1;
    }

    private static int setAssassinHearts(ServerCommandSource source, java.util.Collection<ServerPlayerEntity> players, int hearts) {
        for (ServerPlayerEntity player : players) {
            int set = AssassinState.setAssassinHearts(player, hearts);
            GemPlayerState.applyMaxHearts(player);
            source.sendFeedback(() -> Text.literal("Set assassin hearts=" + set + " for " + player.getName().getString()), true);
        }
        return 1;
    }

    private static int setAssassinCounter(ServerCommandSource source, java.util.Collection<ServerPlayerEntity> players, String counter, int value) {
        int updated = 0;
        for (ServerPlayerEntity player : players) {
            if (AssassinState.setCounter(player, counter, value)) {
                updated++;
                source.sendFeedback(() -> Text.literal("Set assassin counter " + counter + "=" + value + " for " + player.getName().getString()), true);
            } else {
                source.sendError(Text.literal("Unknown assassin counter '" + counter + "'."));
            }
        }
        return updated > 0 ? 1 : 0;
    }

    private static int giveAllGems(ServerCommandSource source, java.util.Collection<ServerPlayerEntity> players) {
        EnumSet<GemId> all = EnumSet.allOf(GemId.class);
        for (ServerPlayerEntity player : players) {
            GemPlayerState.initIfNeeded(player);
            GemPlayerState.setOwnedGemsExact(player, all);
            for (GemId gemId : GemId.values()) {
                ensurePlayerHasItem(player, ModItems.gemItem(gemId));
            }
            resync(player);
            source.sendFeedback(() -> Text.literal("Gave all gems to " + player.getName().getString()), true);
        }
        return 1;
    }

    private static int giveAllLegendaries(ServerCommandSource source, java.util.Collection<ServerPlayerEntity> players) {
        List<Item> items = List.of(
                ModItems.TRACKER_COMPASS,
                ModItems.RECALL_RELIC,
                ModItems.HYPNO_STAFF,
                ModItems.EARTHSPLITTER_PICK,
                ModItems.SUPREME_HELMET,
                ModItems.SUPREME_CHESTPLATE,
                ModItems.SUPREME_LEGGINGS,
                ModItems.SUPREME_BOOTS,
                ModItems.BLOOD_OATH_BLADE,
                ModItems.DEMOLITION_BLADE,
                ModItems.HUNTERS_SIGHT_BOW,
                ModItems.THIRD_STRIKE_BLADE,
                ModItems.VAMPIRIC_EDGE,
                ModItems.EXPERIENCE_BLADE,
                ModItems.REVERSAL_MIRROR,
                ModItems.HUNTERS_TROPHY_NECKLACE,
                ModItems.GLADIATORS_MARK,
                ModItems.SOUL_SHACKLE,
                ModItems.DUELISTS_RAPIER,
                ModItems.CHALLENGERS_GAUNTLET,
                ModItems.CHRONO_CHARM,
                ModItems.GEM_SEER
        );
        for (ServerPlayerEntity player : players) {
            for (Item item : items) {
                ensurePlayerHasItem(player, item);
            }
            source.sendFeedback(() -> Text.literal("Gave all legendaries to " + player.getName().getString()), true);
        }
        return 1;
    }

    private static int setEnhancementPenalty(ServerCommandSource source, java.util.Collection<ServerPlayerEntity> players, int value) {
        for (ServerPlayerEntity player : players) {
            int set = GemPlayerState.setEnergyCapPenalty(player, value);
            resync(player);
            source.sendFeedback(() -> Text.literal("Set enhancement cap penalty=" + set + " for " + player.getName().getString()), true);
        }
        return 1;
    }

    private static int clearGems(ServerCommandSource source, java.util.Collection<ServerPlayerEntity> players) {
        for (ServerPlayerEntity player : players) {
            GemPlayerState.initIfNeeded(player);
            GemId active = GemPlayerState.getActiveGem(player);
            GemPlayerState.setOwnedGemsExact(player, EnumSet.of(active));
            removeGemItems(player, active);
            ensurePlayerHasItem(player, ModItems.gemItem(active));
            resync(player);
            source.sendFeedback(() -> Text.literal("Cleared owned gems for " + player.getName().getString() + " (kept " + active.name() + ")"), true);
        }
        return 1;
    }

    private static void removeGemItems(ServerPlayerEntity player, GemId keep) {
        if (player == null) {
            return;
        }
        var inventory = player.getInventory();
        for (GemId gemId : GemId.values()) {
            if (gemId == keep) {
                continue;
            }
            Item item = ModItems.gemItem(gemId);
            for (int i = 0; i < inventory.size(); i++) {
                ItemStack stack = inventory.getStack(i);
                if (!stack.isEmpty() && stack.isOf(item)) {
                    inventory.setStack(i, ItemStack.EMPTY);
                }
            }
        }
    }

    private static int stressStart(
            ServerCommandSource source,
            java.util.Collection<ServerPlayerEntity> players,
            int seconds,
            int periodTicks,
            GemsStressTest.Mode mode,
            boolean cycleGems,
            boolean forceEnergy10
    ) {
        for (ServerPlayerEntity p : players) {
            GemsStressTest.start(p, seconds, periodTicks, mode, cycleGems, forceEnergy10);
        }
        source.sendFeedback(() -> Text.literal("Started stress test for " + players.size() + " player(s): " +
                seconds + "s, period=" + periodTicks + "t, mode=" + mode + ", cycleGems=" + cycleGems + ", forceEnergy10=" + forceEnergy10), true);
        return 1;
    }

    private static int stressStop(ServerCommandSource source, java.util.Collection<ServerPlayerEntity> players) {
        int stopped = 0;
        for (ServerPlayerEntity p : players) {
            if (GemsStressTest.stop(p.getUuid())) {
                stopped++;
            }
        }
        int finalStopped = stopped;
        source.sendFeedback(() -> Text.literal("Stopped stress test for " + finalStopped + " player(s)."), true);
        return 1;
    }

    private static int trust(ServerPlayerEntity owner, ServerPlayerEntity other) {
        if (owner == other) {
            owner.sendMessage(Text.translatable("gems.trust.always_trusted"), false);
            return 1;
        }
        boolean changed = GemTrust.trust(owner, other.getUuid());
        owner.sendMessage(changed 
                ? Text.translatable("gems.trust.trusted", other.getName().getString())
                : Text.translatable("gems.trust.already_trusted", other.getName().getString()), false);
        return 1;
    }

    private static int untrust(ServerPlayerEntity owner, ServerPlayerEntity other) {
        if (owner == other) {
            owner.sendMessage(Text.translatable("gems.trust.cannot_untrust_self"), false);
            return 0;
        }
        boolean changed = GemTrust.untrust(owner, other.getUuid());
        owner.sendMessage(changed 
                ? Text.translatable("gems.trust.untrusted", other.getName().getString())
                : Text.translatable("gems.trust.was_not_trusted", other.getName().getString()), false);
        return 1;
    }

    private static int trustList(ServerPlayerEntity owner) {
        var trusted = GemTrust.getTrusted(owner);
        if (trusted.isEmpty()) {
            owner.sendMessage(Text.translatable("gems.trust.list_empty"), false);
            return 1;
        }
        owner.sendMessage(Text.translatable("gems.trust.list_header", trusted.size()), false);
        for (var uuid : trusted) {
            owner.sendMessage(Text.literal("- " + uuid), false);
        }
        return 1;
    }

    private static int track(ServerPlayerEntity player, String name) {
        TrackerCompassItem.setTarget(player, name);
        return 1;
    }

    private static String passiveName(Identifier id) {
        GemPassive passive = ModPassives.get(id);
        return passive == null ? id.toString() : passive.name();
    }

    private static String abilityName(Identifier id) {
        GemAbility ability = ModAbilities.get(id);
        return ability == null ? id.toString() : ability.name();
    }

    private static int giveItem(ServerCommandSource source, ServerPlayerEntity player, String rawItem) {
        Item item = switch (rawItem.toLowerCase(Locale.ROOT)) {
            case "heart" -> ModItems.HEART;
            case "energy_upgrade" -> ModItems.ENERGY_UPGRADE;
            case "gem_trader" -> ModItems.GEM_TRADER;
            case "gem_purchase" -> ModItems.GEM_PURCHASE;
            case "tracker_compass" -> ModItems.TRACKER_COMPASS;
            case "recall_relic" -> ModItems.RECALL_RELIC;
            case "hypno_staff" -> ModItems.HYPNO_STAFF;
            case "earthsplitter_pick" -> ModItems.EARTHSPLITTER_PICK;
            case "supreme_helmet" -> ModItems.SUPREME_HELMET;
            case "supreme_chestplate" -> ModItems.SUPREME_CHESTPLATE;
            case "supreme_leggings" -> ModItems.SUPREME_LEGGINGS;
            case "supreme_boots" -> ModItems.SUPREME_BOOTS;
            case "blood_oath_blade" -> ModItems.BLOOD_OATH_BLADE;
            case "demolition_blade" -> ModItems.DEMOLITION_BLADE;
            case "hunters_sight_bow" -> ModItems.HUNTERS_SIGHT_BOW;
            case "third_strike_blade" -> ModItems.THIRD_STRIKE_BLADE;
            case "vampiric_edge" -> ModItems.VAMPIRIC_EDGE;
            default -> null;
        };
        if (item == null) {
            source.sendError(Text.literal("Unknown item '" + rawItem + "'."));
            return 0;
        }
        player.giveItemStack(new ItemStack(item));
        source.sendFeedback(() -> Text.literal("Gave " + rawItem + " to " + player.getName().getString()), true);
        return 1;
    }

    private static int giveGem(ServerCommandSource source, ServerPlayerEntity player, String rawGem) {
        GemId gemId;
        try {
            gemId = GemId.valueOf(rawGem.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            source.sendError(Text.literal("Unknown gem '" + rawGem + "'"));
            return 0;
        }
        GemPlayerState.initIfNeeded(player);
        GemPlayerState.addOwnedGem(player, gemId);
        ensurePlayerHasItem(player, ModItems.gemItem(gemId));
        resync(player);
        source.sendFeedback(() -> Text.literal("Gave gem " + gemId.name() + " to " + player.getName().getString()), true);
        return 1;
    }

    private static int setEnergy(ServerCommandSource source, ServerPlayerEntity player, int energy) {
        GemPlayerState.initIfNeeded(player);
        int set = GemPlayerState.setEnergy(player, energy);
        resync(player);
        source.sendFeedback(() -> Text.literal("Set " + player.getName().getString() + " energy to " + set + "/10"), true);
        return 1;
    }

    private static int setHearts(ServerCommandSource source, ServerPlayerEntity player, int hearts) {
        GemPlayerState.initIfNeeded(player);
        int set = GemPlayerState.setMaxHearts(player, hearts);
        resync(player);
        source.sendFeedback(() -> Text.literal("Set " + player.getName().getString() + " max hearts to " + set + "/20"), true);
        return 1;
    }

    private static int setGem(ServerCommandSource source, ServerPlayerEntity player, String rawGem) {
        GemId gemId;
        try {
            gemId = GemId.valueOf(rawGem.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            source.sendError(Text.literal("Unknown gem '" + rawGem + "'"));
            return 0;
        }

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, gemId);
        ensurePlayerHasItem(player, ModItems.gemItem(gemId));
        resync(player);
        source.sendFeedback(() -> Text.literal("Set " + player.getName().getString() + " active gem to " + gemId.name()), true);
        return 1;
    }

    private static int reset(ServerCommandSource source, ServerPlayerEntity player) {
        GemPlayerState.initIfNeeded(player);
        GemId[] values = GemId.values();
        GemId assigned = values[player.getRandom().nextInt(values.length)];
        GemPlayerState.resetToNew(player, assigned);
        ensurePlayerHasItem(player, ModItems.gemItem(assigned));
        resync(player);
        source.sendFeedback(() -> Text.literal("Reset " + player.getName().getString() + " gem state (assigned " + assigned.name() + ")"), true);
        return 1;
    }

    private static int trade(ServerPlayerEntity player, String rawGem) {
        GemPlayerState.initIfNeeded(player);

        GemId gemId;
        try {
            gemId = GemId.valueOf(rawGem.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            player.sendMessage(Text.translatable("gems.trade.unknown_gem", rawGem), false);
            return 0;
        }

        GemTrading.Result result = GemTrading.trade(player, gemId);
        if (!result.success()) {
            return 0;
        }
        player.sendMessage(Text.translatable("gems.trade.traded_for", gemId.name()), false);
        return 1;
    }

    private static void registerTitleCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("gems")
                .then(CommandManager.literal("admin")
                        .requires(src -> src.getPermissions().hasPermission(new Permission.Level(PermissionLevel.fromLevel(2))))
                        .then(CommandManager.literal("title")
                                .then(CommandManager.literal("set")
                                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                                .then(CommandManager.argument("titleId", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> suggestTitles(builder))
                                                        .executes(ctx -> titleSet(
                                                                ctx.getSource(),
                                                                EntityArgumentType.getPlayers(ctx, "players"),
                                                                StringArgumentType.getString(ctx, "titleId")
                                                        )))))
                                .then(CommandManager.literal("clear")
                                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                                .executes(ctx -> titleClear(
                                                        ctx.getSource(),
                                                        EntityArgumentType.getPlayers(ctx, "players")
                                                ))))
                                .then(CommandManager.literal("list")
                                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                                .executes(ctx -> titleList(
                                                        ctx.getSource(),
                                                        EntityArgumentType.getPlayers(ctx, "players")
                                                ))))
                        )));
    }

    private static void registerAugmentCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("gems")
                .then(CommandManager.literal("admin")
                        .requires(src -> src.getPermissions().hasPermission(new Permission.Level(PermissionLevel.fromLevel(2))))
                        .then(CommandManager.literal("augment")
                                .then(CommandManager.literal("list")
                                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                                .then(CommandManager.argument("gem", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> suggestGems(builder))
                                                        .executes(ctx -> augmentList(
                                                                ctx.getSource(),
                                                                EntityArgumentType.getPlayers(ctx, "players"),
                                                                StringArgumentType.getString(ctx, "gem")
                                                        )))))
                                .then(CommandManager.literal("remove")
                                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                                .then(CommandManager.argument("gem", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> suggestGems(builder))
                                                        .then(CommandManager.argument("slot", IntegerArgumentType.integer(1, 16))
                                                                .executes(ctx -> augmentRemove(
                                                                        ctx.getSource(),
                                                                        EntityArgumentType.getPlayers(ctx, "players"),
                                                                        StringArgumentType.getString(ctx, "gem"),
                                                                        IntegerArgumentType.getInteger(ctx, "slot")
                                                                ))))))
                                .then(CommandManager.literal("clear")
                                        .then(CommandManager.argument("players", EntityArgumentType.players())
                                                .then(CommandManager.argument("gem", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> suggestGems(builder))
                                                        .executes(ctx -> augmentClear(
                                                                ctx.getSource(),
                                                                EntityArgumentType.getPlayers(ctx, "players"),
                                                                StringArgumentType.getString(ctx, "gem")
                                                        )))))
                        )));
    }

    private static int titleSet(ServerCommandSource source, Iterable<ServerPlayerEntity> players, String titleId) {
        MasteryReward reward = MasteryRewards.findById(titleId);
        if (reward == null || reward.type() != MasteryReward.MasteryRewardType.TITLE) {
            source.sendError(Text.translatable("gems.title.admin.invalid", titleId));
            return 0;
        }
        for (ServerPlayerEntity player : players) {
            GemMastery.setSelectedTitle(player, reward.id(), true);
            source.sendFeedback(() -> Text.translatable("gems.title.admin.set", player.getName().getString(), titleId), true);
        }
        return 1;
    }

    private static int titleClear(ServerCommandSource source, Iterable<ServerPlayerEntity> players) {
        for (ServerPlayerEntity player : players) {
            GemMastery.setSelectedTitle(player, "", true);
            source.sendFeedback(() -> Text.translatable("gems.title.admin.cleared", player.getName().getString()), true);
        }
        return 1;
    }

    private static int titleList(ServerCommandSource source, Iterable<ServerPlayerEntity> players) {
        for (ServerPlayerEntity player : players) {
            source.sendFeedback(() -> Text.translatable("gems.title.admin.list_header", player.getName().getString()), false);
            List<Text> lines = new ArrayList<>();
            for (GemId gem : GemId.values()) {
                int usage = GemMastery.getUsage(player, gem);
                for (MasteryReward reward : MasteryRewards.getRewards(gem)) {
                    if (reward.type() != MasteryReward.MasteryRewardType.TITLE) {
                        continue;
                    }
                    boolean unlocked = usage >= reward.threshold();
                    Text name = Text.translatable(reward.displayKey());
                    String status = unlocked ? "unlocked" : "locked";
                    lines.add(Text.literal("- " + reward.id() + " (" + status + "): ").append(name));
                }
            }
            if (lines.isEmpty()) {
                source.sendFeedback(() -> Text.translatable("gems.title.admin.list_empty"), false);
            } else {
                for (Text line : lines) {
                    source.sendFeedback(() -> line, false);
                }
            }
        }
        return 1;
    }

    private static int augmentList(ServerCommandSource source, Iterable<ServerPlayerEntity> players, String rawGem) {
        GemId gemId;
        try {
            gemId = GemId.valueOf(rawGem.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            source.sendError(Text.translatable("gems.augment.admin.invalid_gem", rawGem));
            return 0;
        }

        for (ServerPlayerEntity player : players) {
            List<AugmentInstance> augments = AugmentRuntime.getGemAugments(player, gemId);
            source.sendFeedback(() -> Text.translatable("gems.augment.admin.list_header", player.getName().getString(), gemId.name()), false);
            if (augments.isEmpty()) {
                source.sendFeedback(() -> Text.translatable("gems.augment.admin.list_empty"), false);
            } else {
                for (int i = 0; i < augments.size(); i++) {
                    int slot = i + 1;
                    AugmentInstance inst = augments.get(i);
                    source.sendFeedback(() -> formatAugmentEntry(slot, inst), false);
                }
            }
        }
        return 1;
    }

    private static int augmentRemove(ServerCommandSource source, Iterable<ServerPlayerEntity> players, String rawGem, int slot) {
        GemId gemId;
        try {
            gemId = GemId.valueOf(rawGem.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            source.sendError(Text.translatable("gems.augment.admin.invalid_gem", rawGem));
            return 0;
        }

        int index = slot - 1;
        for (ServerPlayerEntity player : players) {
            boolean removed = AugmentRuntime.removeGemAugment(player, gemId, index);
            if (removed) {
                source.sendFeedback(() -> Text.translatable("gems.augment.admin.removed", player.getName().getString(), slot, gemId.name()), true);
            } else {
                source.sendError(Text.translatable("gems.augment.admin.remove_failed", player.getName().getString(), slot, gemId.name()));
            }
        }
        return 1;
    }

    private static int augmentClear(ServerCommandSource source, Iterable<ServerPlayerEntity> players, String rawGem) {
        GemId gemId;
        try {
            gemId = GemId.valueOf(rawGem.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            source.sendError(Text.translatable("gems.augment.admin.invalid_gem", rawGem));
            return 0;
        }

        for (ServerPlayerEntity player : players) {
            AugmentRuntime.clearGemAugments(player, gemId);
            source.sendFeedback(() -> Text.translatable("gems.augment.admin.cleared", player.getName().getString(), gemId.name()), true);
        }
        return 1;
    }

    private static Text formatAugmentEntry(int slot, AugmentInstance instance) {
        AugmentDefinition def = AugmentRegistry.get(instance.augmentId());
        String name = def != null ? Text.translatable(def.nameKey()).getString() : instance.augmentId();
        String mag = String.format(Locale.ROOT, "%.2f", instance.magnitude());
        Text label = Text.literal("#" + slot + " ").formatted(Formatting.GRAY)
                .append(Text.literal(name).formatted(rarityColor(instance.rarity())))
                .append(Text.literal(" (" + instance.rarity().name() + " x" + mag + ")").formatted(Formatting.DARK_GRAY));
        return label;
    }

    private static Formatting rarityColor(AugmentRarity rarity) {
        return switch (rarity) {
            case COMMON -> Formatting.GRAY;
            case RARE -> Formatting.AQUA;
            case EPIC -> Formatting.LIGHT_PURPLE;
        };
    }

    private static void resync(ServerPlayerEntity player) {
        GemPlayerState.applyMaxHearts(player);
        GemPowers.sync(player);
        GemItemGlint.sync(player);
        GemStateSync.send(player);
    }

    private static void ensurePlayerHasItem(ServerPlayerEntity player, Item item) {
        for (ItemStack stack : player.getInventory().getMainStacks()) {
            if (stack.isOf(item)) {
                return;
            }
        }
        if (player.getOffHandStack().isOf(item)) {
            return;
        }
        if (player.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD).isOf(item)
                || player.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST).isOf(item)
                || player.getEquippedStack(net.minecraft.entity.EquipmentSlot.LEGS).isOf(item)
                || player.getEquippedStack(net.minecraft.entity.EquipmentSlot.FEET).isOf(item)) {
            return;
        }
        player.giveItemStack(new ItemStack(item));
    }

    private static CompletableFuture<Suggestions> suggestOnlinePlayers(ServerCommandSource source, SuggestionsBuilder builder) {
        for (ServerPlayerEntity player : source.getServer().getPlayerManager().getPlayerList()) {
            builder.suggest(player.getName().getString());
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestGems(SuggestionsBuilder builder) {
        for (GemId gemId : GemId.values()) {
            builder.suggest(gemId.name().toLowerCase(Locale.ROOT));
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestAdminItems(SuggestionsBuilder builder) {
        for (String id : List.of(
                "heart",
                "energy_upgrade",
                "gem_trader",
                "gem_purchase",
                "tracker_compass",
                "recall_relic",
                "hypno_staff",
                "earthsplitter_pick",
                "supreme_helmet",
                "supreme_chestplate",
                "supreme_leggings",
                "supreme_boots",
                "blood_oath_blade",
                "demolition_blade",
                "hunters_sight_bow",
                "third_strike_blade",
                "vampiric_edge"
        )) {
            builder.suggest(id);
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestTitles(SuggestionsBuilder builder) {
        for (GemId gem : GemId.values()) {
            for (MasteryReward reward : MasteryRewards.getRewards(gem)) {
                if (reward.type() == MasteryReward.MasteryRewardType.TITLE) {
                    builder.suggest(reward.id());
                }
            }
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestStressMode(SuggestionsBuilder builder) {
        builder.suggest("realistic");
        builder.suggest("force");
        return builder.buildFuture();
    }
}
