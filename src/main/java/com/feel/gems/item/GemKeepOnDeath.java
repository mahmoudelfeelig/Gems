package com.feel.gems.item;

import com.feel.gems.core.GemId;
import com.feel.gems.item.GemOwnership;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.state.GemsPersistentDataHolder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;




/**
 * Keeps gem items from dropping on death by stashing them into persistent player NBT and restoring on respawn.
 */
public final class GemKeepOnDeath {
    private static final String KEY_KEPT_GEMS = "keptGems";

    private GemKeepOnDeath() {
    }

    public static void stash(ServerPlayerEntity player) {
        NbtCompound root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        root.remove(KEY_KEPT_GEMS);

        GemPlayerState.initIfNeeded(player);
        GemId active = GemPlayerState.getActiveGem(player);

        List<ItemStack> kept = new ArrayList<>();
        boolean found = false;

        for (int i = 0; i < player.getInventory().main.size(); i++) {
            ItemStack stack = player.getInventory().main.get(i);
            if (!found && stack.getItem() instanceof GemItem gem && gem.gemId() == active) {
                kept.add(stack.copy());
                player.getInventory().main.set(i, ItemStack.EMPTY);
                found = true;
            }
        }
        for (int i = 0; i < player.getInventory().offHand.size(); i++) {
            ItemStack stack = player.getInventory().offHand.get(i);
            if (!found && stack.getItem() instanceof GemItem gem && gem.gemId() == active) {
                kept.add(stack.copy());
                player.getInventory().offHand.set(i, ItemStack.EMPTY);
                found = true;
            }
        }
        for (int i = 0; i < player.getInventory().armor.size(); i++) {
            ItemStack stack = player.getInventory().armor.get(i);
            if (!found && stack.getItem() instanceof GemItem gem && gem.gemId() == active) {
                kept.add(stack.copy());
                player.getInventory().armor.set(i, ItemStack.EMPTY);
                found = true;
            }
        }

        if (kept.isEmpty()) {
            return;
        }

        SimpleInventory inv = new SimpleInventory(kept.size());
        for (int i = 0; i < kept.size(); i++) {
            inv.setStack(i, kept.get(i));
        }
        NbtList list = inv.toNbtList(player.getServerWorld().getRegistryManager());
        root.put(KEY_KEPT_GEMS, list);
    }

    public static void restore(ServerPlayerEntity player) {
        NbtCompound root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        if (!root.contains(KEY_KEPT_GEMS, NbtElement.LIST_TYPE)) {
            return;
        }
        NbtList list = root.getList(KEY_KEPT_GEMS, NbtElement.COMPOUND_TYPE);
        root.remove(KEY_KEPT_GEMS);
        if (list.isEmpty()) {
            return;
        }

        SimpleInventory inv = new SimpleInventory(list.size());
        inv.readNbtList(list, player.getServerWorld().getRegistryManager());
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty()) {
                GemOwnership.tagOwned(stack, player.getUuid(), GemPlayerState.getGemEpoch(player));
                player.giveItemStack(stack);
            }
        }
    }
}
