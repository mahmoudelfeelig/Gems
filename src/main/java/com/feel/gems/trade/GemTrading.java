package com.feel.gems.trade;

import com.feel.gems.item.GemItemGlint;
import com.feel.gems.item.GemOwnership;
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

    public record PurchaseResult(boolean success, boolean consumedToken, boolean alreadyOwned) {
    }

    public static Result trade(ServerPlayerEntity player, GemId gemId) {
        GemPlayerState.initIfNeeded(player);

        boolean consumedTrader = consumeTrader(player);
        if (!consumedTrader) {
            player.sendMessage(Text.literal("You need a Gem Trader to trade."), true);
            return new Result(false, false, false);
        }
        GemId activeBefore = GemPlayerState.getActiveGem(player);
        EnumSet<GemId> ownedBefore = GemPlayerState.getOwnedGems(player);
        boolean alreadyOwned = ownedBefore.contains(gemId);

        EnumSet<GemId> newOwned = EnumSet.copyOf(ownedBefore);
        newOwned.remove(activeBefore); // trade away only the active gem
        newOwned.add(gemId);

        GemPlayerState.setActiveGem(player, gemId);
        GemPlayerState.setOwnedGemsExact(player, newOwned);

        GemPowers.sync(player);
        GemItemGlint.sync(player);
        GemStateSync.send(player);
        Item keep = ModItems.gemItem(gemId);
        Item tradedAway = ModItems.gemItem(activeBefore);
        removeGemItems(player, tradedAway, keep);
        ensurePlayerHasItem(player, keep);

        return new Result(true, true, alreadyOwned);
    }

    public static PurchaseResult purchase(ServerPlayerEntity player, GemId gemId) {
        GemPlayerState.initIfNeeded(player);

        boolean consumedToken = consumePurchaseToken(player);
        if (!consumedToken) {
            player.sendMessage(Text.literal("You need a Gem Purchase Token to buy a gem."), true);
            return new PurchaseResult(false, false, false);
        }

        EnumSet<GemId> ownedBefore = GemPlayerState.getOwnedGems(player);
        boolean alreadyOwned = ownedBefore.contains(gemId);
        EnumSet<GemId> newOwned = EnumSet.copyOf(ownedBefore);
        newOwned.add(gemId);

        GemPlayerState.setActiveGem(player, gemId);
        GemPlayerState.setOwnedGemsExact(player, newOwned);

        GemPowers.sync(player);
        GemItemGlint.sync(player);
        GemStateSync.send(player);

        Item keep = ModItems.gemItem(gemId);
        ensurePlayerHasItem(player, keep);

        return new PurchaseResult(true, true, alreadyOwned);
    }

    private static boolean consumeTrader(ServerPlayerEntity player) {
        Item trader = ModItems.GEM_TRADER;
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

    private static boolean consumePurchaseToken(ServerPlayerEntity player) {
        Item token = ModItems.GEM_PURCHASE;
        ItemStack main = player.getMainHandStack();
        if (main.isOf(token)) {
            main.decrement(1);
            return true;
        }
        ItemStack off = player.getOffHandStack();
        if (off.isOf(token)) {
            off.decrement(1);
            return true;
        }
        for (int i = 0; i < player.getInventory().main.size(); i++) {
            ItemStack stack = player.getInventory().main.get(i);
            if (stack.isOf(token)) {
                stack.decrement(1);
                if (stack.isEmpty()) {
                    player.getInventory().main.set(i, ItemStack.EMPTY);
                }
                return true;
            }
        }
        for (int i = 0; i < player.getInventory().offHand.size(); i++) {
            ItemStack stack = player.getInventory().offHand.get(i);
            if (stack.isOf(token)) {
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
        ItemStack stack = new ItemStack(item);
        GemOwnership.tagOwned(stack, player.getUuid(), GemPlayerState.getGemEpoch(player));
        player.giveItemStack(stack);
    }

    private static void removeGemItems(ServerPlayerEntity player, Item toRemove, Item toKeep) {
        purgeInv(player.getInventory().main, toRemove, toKeep);
        purgeInv(player.getInventory().offHand, toRemove, toKeep);
        purgeInv(player.getInventory().armor, toRemove, toKeep);
    }

    private static void purgeInv(java.util.List<ItemStack> stacks, Item toRemove, Item toKeep) {
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (!(stack.getItem() instanceof com.feel.gems.item.GemItem)) {
                continue;
            }
            if (stack.isOf(toKeep)) {
                continue;
            }
            if (!stack.isOf(toRemove)) {
                continue;
            }
            stacks.set(i, ItemStack.EMPTY);
        }
    }
}
