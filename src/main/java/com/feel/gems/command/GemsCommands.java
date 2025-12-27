package com.feel.gems.command;

import com.feel.gems.assassin.AssassinState;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemDefinition;
import com.feel.gems.core.GemEnergyState;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.debug.GemsPerfMonitor;
import com.feel.gems.debug.GemsStressTest;
import com.feel.gems.item.GemItemGlint;
import com.feel.gems.item.ModItems;
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
import com.feel.gems.trade.GemTrading;
import com.feel.gems.trust.GemTrust;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;




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
                                .requires(src -> src.hasPermissionLevel(2))
                                .executes(ctx -> reloadBalance(ctx.getSource())))
                        .then(CommandManager.literal("dumpBalance")
                                .requires(src -> src.hasPermissionLevel(2))
                                .executes(ctx -> dumpBalance(ctx.getSource())))
                        .then(CommandManager.literal("trust")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .executes(ctx -> trust(ctx.getSource().getPlayerOrThrow(), EntityArgumentType.getPlayer(ctx, "player")))))
                        .then(CommandManager.literal("untrust")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .executes(ctx -> untrust(ctx.getSource().getPlayerOrThrow(), EntityArgumentType.getPlayer(ctx, "player")))))
                        .then(CommandManager.literal("trustlist")
                                .executes(ctx -> trustList(ctx.getSource().getPlayerOrThrow())))
                        .then(CommandManager.literal("summoner")
                            .then(CommandManager.literal("loadout")
                                .executes(ctx -> openSummonerLoadout(ctx.getSource().getPlayerOrThrow()))))
                        .then(CommandManager.literal("trade")
                                .then(CommandManager.argument("gem", StringArgumentType.word())
                                        .suggests((ctx, builder) -> suggestGems(builder))
                                        .executes(ctx -> trade(ctx.getSource().getPlayerOrThrow(), StringArgumentType.getString(ctx, "gem")))))
                        .then(CommandManager.literal("admin")
                                .requires(src -> src.hasPermissionLevel(2))
                                .then(CommandManager.literal("status")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(ctx -> status(ctx.getSource(), EntityArgumentType.getPlayer(ctx, "player")))))
                                .then(CommandManager.literal("cast")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .then(CommandManager.argument("slot", IntegerArgumentType.integer(1, 10))
                                                        .executes(ctx -> castSlot(
                                                                ctx.getSource(),
                                                                EntityArgumentType.getPlayer(ctx, "player"),
                                                                IntegerArgumentType.getInteger(ctx, "slot")
                                                        )))))
                                .then(CommandManager.literal("resync")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(ctx -> {
                                                    ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");
                                                    GemPlayerState.initIfNeeded(player);
                                                    resync(player);
                                                    ctx.getSource().sendFeedback(() -> Text.literal("Resynced " + player.getName().getString()), true);
                                                    return 1;
                                                })))
                                .then(CommandManager.literal("giveItem")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .then(CommandManager.argument("item", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> suggestAdminItems(builder))
                                                        .executes(ctx -> giveItem(
                                                                ctx.getSource(),
                                                                EntityArgumentType.getPlayer(ctx, "player"),
                                                                StringArgumentType.getString(ctx, "item")
                                                        )))))
                                .then(CommandManager.literal("giveGem")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .then(CommandManager.argument("gem", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> suggestGems(builder))
                                                        .executes(ctx -> giveGem(
                                                                ctx.getSource(),
                                                                EntityArgumentType.getPlayer(ctx, "player"),
                                                                StringArgumentType.getString(ctx, "gem")
                                                        )))))
                                .then(CommandManager.literal("setEnergy")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .then(CommandManager.argument("energy", IntegerArgumentType.integer(GemPlayerState.MIN_ENERGY, GemPlayerState.MAX_ENERGY))
                                                        .executes(ctx -> setEnergy(
                                                                ctx.getSource(),
                                                                EntityArgumentType.getPlayer(ctx, "player"),
                                                                IntegerArgumentType.getInteger(ctx, "energy")
                                                        )))))
                                .then(CommandManager.literal("setHearts")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .then(CommandManager.argument("hearts", IntegerArgumentType.integer(GemPlayerState.MIN_MAX_HEARTS, GemPlayerState.MAX_MAX_HEARTS))
                                                        .executes(ctx -> setHearts(
                                                                ctx.getSource(),
                                                                EntityArgumentType.getPlayer(ctx, "player"),
                                                                IntegerArgumentType.getInteger(ctx, "hearts")
                                                        )))))
                                .then(CommandManager.literal("setGem")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .then(CommandManager.argument("gem", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> suggestGems(builder))
                                                        .executes(ctx -> setGem(
                                                                ctx.getSource(),
                                                                EntityArgumentType.getPlayer(ctx, "player"),
                                                                StringArgumentType.getString(ctx, "gem")
                                                        )))))
                                .then(CommandManager.literal("reset")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(ctx -> reset(ctx.getSource(), EntityArgumentType.getPlayer(ctx, "player")))))
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
            source.sendFeedback(() -> Text.literal("Assassin: YES (hearts=" + AssassinState.getAssassinHearts(player) + "/10, eliminated=" + AssassinState.isEliminated(player) + ")"), false);
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

    private static int openSummonerLoadout(ServerPlayerEntity player) {
        com.feel.gems.net.ServerSummonerNetworking.openEditor(player);
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
            owner.sendMessage(Text.literal("You are always trusted."), false);
            return 1;
        }
        boolean changed = GemTrust.trust(owner, other.getUuid());
        owner.sendMessage(Text.literal(changed ? "Trusted " + other.getName().getString() : other.getName().getString() + " is already trusted."), false);
        return 1;
    }

    private static int untrust(ServerPlayerEntity owner, ServerPlayerEntity other) {
        if (owner == other) {
            owner.sendMessage(Text.literal("You cannot untrust yourself."), false);
            return 0;
        }
        boolean changed = GemTrust.untrust(owner, other.getUuid());
        owner.sendMessage(Text.literal(changed ? "Untrusted " + other.getName().getString() : other.getName().getString() + " was not trusted."), false);
        return 1;
    }

    private static int trustList(ServerPlayerEntity owner) {
        var trusted = GemTrust.getTrusted(owner);
        if (trusted.isEmpty()) {
            owner.sendMessage(Text.literal("Trusted: -"), false);
            return 1;
        }
        owner.sendMessage(Text.literal("Trusted (" + trusted.size() + "):"), false);
        for (var uuid : trusted) {
            owner.sendMessage(Text.literal("- " + uuid), false);
        }
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
            default -> null;
        };
        if (item == null) {
            source.sendError(Text.literal("Unknown item '" + rawItem + "'. Use: heart, energy_upgrade, gem_trader, gem_purchase"));
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
            player.sendMessage(Text.literal("Unknown gem '" + rawGem + "'"), false);
            return 0;
        }

        GemTrading.Result result = GemTrading.trade(player, gemId);
        if (!result.success()) {
            return 0;
        }
        player.sendMessage(Text.literal("Traded for " + gemId.name()), false);
        return 1;
    }

    private static void resync(ServerPlayerEntity player) {
        GemPlayerState.applyMaxHearts(player);
        GemPowers.sync(player);
        GemItemGlint.sync(player);
        GemStateSync.send(player);
    }

    private static void ensurePlayerHasItem(ServerPlayerEntity player, Item item) {
        for (ItemStack stack : player.getInventory().main) {
            if (stack.isOf(item)) {
                return;
            }
        }
        for (ItemStack stack : player.getInventory().offHand) {
            if (stack.isOf(item)) {
                return;
            }
        }
        for (ItemStack stack : player.getInventory().armor) {
            if (stack.isOf(item)) {
                return;
            }
        }
        player.giveItemStack(new ItemStack(item));
    }

    private static CompletableFuture<Suggestions> suggestGems(SuggestionsBuilder builder) {
        for (GemId gemId : GemId.values()) {
            builder.suggest(gemId.name().toLowerCase(Locale.ROOT));
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestAdminItems(SuggestionsBuilder builder) {
        for (String id : List.of("heart", "energy_upgrade", "gem_trader", "gem_purchase")) {
            builder.suggest(id);
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestStressMode(SuggestionsBuilder builder) {
        builder.suggest("realistic");
        builder.suggest("force");
        return builder.buildFuture();
    }
}
