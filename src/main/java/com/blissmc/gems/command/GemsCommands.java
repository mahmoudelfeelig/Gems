package com.blissmc.gems.command;

import com.blissmc.gems.core.GemDefinition;
import com.blissmc.gems.core.GemEnergyState;
import com.blissmc.gems.core.GemId;
import com.blissmc.gems.core.GemRegistry;
import com.blissmc.gems.item.ModItems;
import com.blissmc.gems.net.GemStateSync;
import com.blissmc.gems.power.GemAbility;
import com.blissmc.gems.power.GemPassive;
import com.blissmc.gems.power.GemPowers;
import com.blissmc.gems.power.ModAbilities;
import com.blissmc.gems.power.ModPassives;
import com.blissmc.gems.state.GemPlayerState;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class GemsCommands {
    private GemsCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("gems")
                .then(CommandManager.literal("status")
                        .executes(ctx -> status(ctx.getSource().getPlayerOrThrow())))
                .then(CommandManager.literal("trade")
                        .then(CommandManager.argument("gem", StringArgumentType.word())
                                .suggests((ctx, builder) -> suggestGems(builder))
                                .executes(ctx -> trade(ctx.getSource().getPlayerOrThrow(), StringArgumentType.getString(ctx, "gem"))))));
    }

    private static int status(ServerPlayerEntity player) {
        GemPlayerState.initIfNeeded(player);

        GemId active = GemPlayerState.getActiveGem(player);
        int energy = GemPlayerState.getEnergy(player);
        int hearts = GemPlayerState.getMaxHearts(player);

        GemDefinition def = GemRegistry.definition(active);
        GemEnergyState energyState = new GemEnergyState(energy);

        List<Identifier> passives = def.availablePassives(energyState);
        List<Identifier> abilities = def.availableAbilities(energyState);

        player.sendMessage(Text.literal("Gem: " + active.name()), false);
        player.sendMessage(Text.literal("Energy: " + energyState.tier().name() + " (" + energy + "/10)"), false);
        player.sendMessage(Text.literal("Max hearts: " + hearts + "/20"), false);
        player.sendMessage(Text.literal("Passives: " + (passives.isEmpty() ? "-" : passives.stream().map(GemsCommands::passiveName).reduce((a, b) -> a + ", " + b).orElse("-"))), false);
        player.sendMessage(Text.literal("Abilities: " + (abilities.isEmpty() ? "-" : abilities.stream().map(GemsCommands::abilityName).reduce((a, b) -> a + ", " + b).orElse("-"))), false);
        player.sendMessage(Text.literal("Owned: " + GemPlayerState.getOwnedGems(player)), false);
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

    private static int trade(ServerPlayerEntity player, String rawGem) {
        GemPlayerState.initIfNeeded(player);

        GemId gemId;
        try {
            gemId = GemId.valueOf(rawGem.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            player.sendMessage(Text.literal("Unknown gem '" + rawGem + "'"), false);
            return 0;
        }

        boolean alreadyOwned = GemPlayerState.getOwnedGems(player).contains(gemId);
        if (!alreadyOwned) {
            ItemStack traderStack = findTraderStack(player);
            if (traderStack == null) {
                player.sendMessage(Text.literal("Hold a Gem Trader to trade for a new gem."), false);
                return 0;
            }
            traderStack.decrement(1);
            GemPlayerState.addOwnedGem(player, gemId);
        }

        GemPlayerState.setActiveGem(player, gemId);
        GemPowers.sync(player);
        GemStateSync.send(player);
        ensurePlayerHasItem(player, ModItems.gemItem(gemId));
        player.sendMessage(Text.literal(alreadyOwned ? "Switched active gem to " + gemId.name() : "Traded for " + gemId.name()), false);
        return 1;
    }

    private static ItemStack findTraderStack(ServerPlayerEntity player) {
        Item trader = ModItems.TRADER;
        ItemStack main = player.getMainHandStack();
        if (main.isOf(trader)) {
            return main;
        }
        ItemStack off = player.getOffHandStack();
        if (off.isOf(trader)) {
            return off;
        }
        return null;
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
}
