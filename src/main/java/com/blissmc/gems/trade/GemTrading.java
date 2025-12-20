package com.feel.gems.trade;

import com.feel.gems.item.GemItemGlint;
import com.feel.gems.item.ModItems;
import com.feel.gems.net.GemStateSync;
import com.feel.gems.power.GemPowers;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.core.GemId;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.EnumSet;

public final class GemTrading {
    private GemTrading() {
    }

    public record Result(boolean success, boolean consumedTrader, boolean alreadyOwned) {
    }

    public static Result trade(ServerPlayerEntity player, GemId gemId) {
        GemPlayerState.initIfNeeded(player);

        boolean consumedTrader = consumeTrader(player);
        if (!consumedTrader) {
            player.sendMessage(Text.literal("You need a Gem Trader to trade."), true);
            return new Result(false, false, false);
        }

        EnumSet<GemId> ownedBefore = GemPlayerState.getOwnedGems(player);
        boolean alreadyOwned = ownedBefore.contains(gemId);

        // Trading is a "wipe + reroll": keep only the newly selected gem.
        GemPlayerState.setOwnedGemsExact(player, EnumSet.of(gemId));
        GemPlayerState.setActiveGem(player, gemId);
        pruneGemItems(player, gemId);

        GemPowers.sync(player);
        GemItemGlint.sync(player);
        GemStateSync.send(player);
        ensurePlayerHasItem(player, ModItems.gemItem(gemId));

        return new Result(true, true, alreadyOwned);
    }

    private static boolean consumeTrader(ServerPlayerEntity player) {
        Item trader = ModItems.TRADER;
        ItemStack main = player.getMainHandStack();
        if (main.isOf(trader)) {
            main.decrement(1);
            return true;
        }
        ItemStack off = player.getOffHandStack();
        if (off.isOf(trader)) {
            off.decrement(1);
            return true;
        }
        for (int i = 0; i < player.getInventory().main.size(); i++) {
            ItemStack stack = player.getInventory().main.get(i);
            if (stack.isOf(trader)) {
                stack.decrement(1);
                if (stack.isEmpty()) {
                    player.getInventory().main.set(i, ItemStack.EMPTY);
                }
                return true;
            }
        }
        for (int i = 0; i < player.getInventory().offHand.size(); i++) {
            ItemStack stack = player.getInventory().offHand.get(i);
            if (stack.isOf(trader)) {
                stack.decrement(1);
                if (stack.isEmpty()) {
                    player.getInventory().offHand.set(i, ItemStack.EMPTY);
                }
                return true;
            }
        }
        return false;
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

    private static void pruneGemItems(ServerPlayerEntity player, GemId keep) {
        boolean kept = false;

        for (int i = 0; i < player.getInventory().main.size(); i++) {
            ItemStack stack = player.getInventory().main.get(i);
            kept = keepOrRemoveGemStack(player.getInventory().main, i, stack, keep, kept);
        }
        for (int i = 0; i < player.getInventory().offHand.size(); i++) {
            ItemStack stack = player.getInventory().offHand.get(i);
            kept = keepOrRemoveGemStack(player.getInventory().offHand, i, stack, keep, kept);
        }
        for (int i = 0; i < player.getInventory().armor.size(); i++) {
            ItemStack stack = player.getInventory().armor.get(i);
            kept = keepOrRemoveGemStack(player.getInventory().armor, i, stack, keep, kept);
        }
    }

    private static boolean keepOrRemoveGemStack(java.util.List<ItemStack> list, int index, ItemStack stack, GemId keep, boolean kept) {
        if (!(stack.getItem() instanceof com.feel.gems.item.GemItem gemItem)) {
            return kept;
        }
        if (gemItem.gemId() == keep && !kept) {
            return true;
        }
        list.set(index, ItemStack.EMPTY);
        return kept;
    }
}
