package com.feel.gems.trade;

import com.feel.gems.core.GemId;
import com.feel.gems.config.GemsDisables;
import com.feel.gems.item.GemItemGlint;
import com.feel.gems.item.GemOwnership;
import com.feel.gems.item.ModItems;
import com.feel.gems.net.GemStateSync;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import java.util.EnumSet;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;




public final class GemTrading {
    private GemTrading() {
    }

    public record Result(boolean success, boolean consumedTrader, boolean alreadyOwned) {
    }

    public record PurchaseResult(boolean success, boolean consumedToken, boolean alreadyOwned) {
    }

    public static Result trade(ServerPlayerEntity player, GemId gemId) {
        GemPlayerState.initIfNeeded(player);

        if (GemsDisables.isGemDisabledFor(player, gemId)) {
            player.sendMessage(Text.translatable("gems.trade.gem_disabled"), true);
            return new Result(false, false, false);
        }

        GemId activeBefore = GemPlayerState.getActiveGem(player);
        if (gemId == activeBefore) {
            player.sendMessage(Text.translatable("gems.trade.gem_already_active"), true);
            return new Result(false, false, true);
        }

        Item activeItem = ModItems.gemItem(activeBefore);
        if (!hasGemOnPlayer(player, activeItem)) {
            player.sendMessage(Text.translatable("gems.trade.need_active_gem"), true);
            return new Result(false, false, false);
        }

        boolean consumedTrader = consumeTrader(player);
        if (!consumedTrader) {
            player.sendMessage(Text.translatable("gems.trade.need_gem_trader"), true);
            return new Result(false, false, false);
        }
        EnumSet<GemId> ownedBefore = GemPlayerState.getOwnedGems(player);
        boolean alreadyOwned = ownedBefore.contains(gemId);

        EnumSet<GemId> newOwned = EnumSet.copyOf(ownedBefore);
        newOwned.remove(activeBefore); // trade away only the active gem
        newOwned.add(gemId);

        GemPlayerState.setActiveGem(player, gemId);
        GemPlayerState.setOwnedGemsExact(player, newOwned);

        GemPowers.sync(player);
        GemStateSync.send(player);
        Item keep = ModItems.gemItem(gemId);
        Item tradedAway = activeItem;
        removeGemItems(player, tradedAway, keep);
        ensurePlayerHasItem(player, keep);
        GemItemGlint.sync(player);

        return new Result(true, true, alreadyOwned);
    }

    public static PurchaseResult purchase(ServerPlayerEntity player, GemId gemId) {
        GemPlayerState.initIfNeeded(player);

        if (GemsDisables.isGemDisabledFor(player, gemId)) {
            player.sendMessage(Text.translatable("gems.trade.gem_disabled"), true);
            return new PurchaseResult(false, false, false);
        }

        boolean consumedToken = consumePurchaseToken(player);
        if (!consumedToken) {
            player.sendMessage(Text.translatable("gems.trade.need_purchase_token"), true);
            return new PurchaseResult(false, false, false);
        }

        EnumSet<GemId> ownedBefore = GemPlayerState.getOwnedGems(player);
        boolean alreadyOwned = ownedBefore.contains(gemId);
        EnumSet<GemId> newOwned = EnumSet.copyOf(ownedBefore);
        newOwned.add(gemId);

        GemPlayerState.setActiveGem(player, gemId);
        GemPlayerState.setOwnedGemsExact(player, newOwned);

        GemPowers.sync(player);
        GemStateSync.send(player);

        Item keep = ModItems.gemItem(gemId);
        ensurePlayerHasItem(player, keep);
        GemItemGlint.sync(player);

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
        var mainStacks = player.getInventory().getMainStacks();
        for (int i = 0; i < mainStacks.size(); i++) {
            ItemStack stack = mainStacks.get(i);
            if (stack.isOf(trader)) {
                stack.decrement(1);
                if (stack.isEmpty()) {
                    mainStacks.set(i, ItemStack.EMPTY);
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
        var mainStacks = player.getInventory().getMainStacks();
        for (int i = 0; i < mainStacks.size(); i++) {
            ItemStack stack = mainStacks.get(i);
            if (stack.isOf(token)) {
                stack.decrement(1);
                if (stack.isEmpty()) {
                    mainStacks.set(i, ItemStack.EMPTY);
                }
                return true;
            }
        }
        return false;
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
        ItemStack stack = new ItemStack(item);
        GemOwnership.tagOwned(stack, player);
        player.giveItemStack(stack);
    }

    private static void removeGemItems(ServerPlayerEntity player, Item toRemove, Item toKeep) {
        purgeInv(player.getInventory().getMainStacks(), toRemove, toKeep);
        purgeEquipment(player, net.minecraft.entity.EquipmentSlot.OFFHAND, toRemove, toKeep);
        purgeEquipment(player, net.minecraft.entity.EquipmentSlot.HEAD, toRemove, toKeep);
        purgeEquipment(player, net.minecraft.entity.EquipmentSlot.CHEST, toRemove, toKeep);
        purgeEquipment(player, net.minecraft.entity.EquipmentSlot.LEGS, toRemove, toKeep);
        purgeEquipment(player, net.minecraft.entity.EquipmentSlot.FEET, toRemove, toKeep);
        purgeInventory(player.getEnderChestInventory(), toRemove, toKeep);
    }

    private static void purgeEquipment(ServerPlayerEntity player, net.minecraft.entity.EquipmentSlot slot, Item toRemove, Item toKeep) {
        ItemStack stack = player.getEquippedStack(slot);
        if (stack.isEmpty()) {
            return;
        }
        if (!(stack.getItem() instanceof com.feel.gems.item.GemItem)) {
            return;
        }
        if (stack.isOf(toKeep) || !stack.isOf(toRemove)) {
            return;
        }
        player.equipStack(slot, ItemStack.EMPTY);
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

    private static void purgeInventory(net.minecraft.inventory.Inventory inv, Item toRemove, Item toKeep) {
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
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
            inv.setStack(i, ItemStack.EMPTY);
        }
    }

    private static boolean hasGemOnPlayer(ServerPlayerEntity player, Item item) {
        if (item == null) {
            return false;
        }
        for (ItemStack stack : player.getInventory().getMainStacks()) {
            if (stack.isOf(item)) {
                return true;
            }
        }
        if (player.getOffHandStack().isOf(item)) {
            return true;
        }
        if (player.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD).isOf(item)
                || player.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST).isOf(item)
                || player.getEquippedStack(net.minecraft.entity.EquipmentSlot.LEGS).isOf(item)
                || player.getEquippedStack(net.minecraft.entity.EquipmentSlot.FEET).isOf(item)) {
            return true;
        }
        return false;
    }
}
