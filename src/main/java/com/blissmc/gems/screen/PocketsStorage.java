package com.blissmc.gems.screen;

import com.blissmc.gems.state.GemsPersistentDataHolder;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;

public final class PocketsStorage {
    private static final String KEY_POCKETS = "pocketsInv";

    private PocketsStorage() {
    }

    public static SimpleInventory load(ServerPlayerEntity player) {
        SimpleInventory inv = new SimpleInventory(9);
        NbtCompound root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        if (root.contains(KEY_POCKETS, NbtElement.LIST_TYPE)) {
            NbtList list = root.getList(KEY_POCKETS, NbtElement.COMPOUND_TYPE);
            inv.readNbtList(list, player.getServerWorld().getRegistryManager());
        }
        return inv;
    }

    public static void save(ServerPlayerEntity player, SimpleInventory inv) {
        NbtCompound root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        NbtList list = inv.toNbtList(player.getServerWorld().getRegistryManager());
        root.put(KEY_POCKETS, list);
    }
}

