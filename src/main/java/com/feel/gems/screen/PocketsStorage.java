package com.feel.gems.screen;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.state.GemsPersistentDataHolder;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.storage.NbtReadView;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.util.ErrorReporter;
import net.minecraft.item.ItemStack;




public final class PocketsStorage {
    private static final String KEY_POCKETS = "pocketsInv";
    private static final String KEY_ITEMS = "items";

    private PocketsStorage() {
    }

    public static SimpleInventory load(ServerPlayerEntity player) {
        int rows = Math.max(1, GemsBalance.v().wealth().pocketsRows());
        SimpleInventory inv = new SimpleInventory(rows * 9);
        NbtCompound root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        NbtCompound data = root.getCompound(KEY_POCKETS).orElse(null);
        if (data != null) {
            var view = NbtReadView.create(ErrorReporter.EMPTY, player.getEntityWorld().getRegistryManager(), data);
            inv.readDataList(view.getTypedListView(KEY_ITEMS, ItemStack.OPTIONAL_CODEC));
        }
        return inv;
    }

    public static void save(ServerPlayerEntity player, SimpleInventory inv) {
        NbtCompound root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        var view = NbtWriteView.create(ErrorReporter.EMPTY, player.getEntityWorld().getRegistryManager());
        inv.toDataList(view.getListAppender(KEY_ITEMS, ItemStack.OPTIONAL_CODEC));
        root.put(KEY_POCKETS, view.getNbt());
    }
}

