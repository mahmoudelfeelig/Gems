package com.blissmc.gems.power;

import com.blissmc.gems.state.GemsPersistentDataHolder;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class ItemLock {
    private static final String KEY_LOCK_UNTIL = "itemLockUntil";
    private static final String KEY_LOCK_ITEM = "itemLockItem";

    private ItemLock() {
    }

    public static void lock(ServerPlayerEntity target, ItemStack item, int durationTicks) {
        if (item.isEmpty() || durationTicks <= 0) {
            return;
        }
        Identifier id = Registries.ITEM.getId(item.getItem());
        if (id == null) {
            return;
        }
        NbtCompound nbt = persistent(target);
        nbt.putLong(KEY_LOCK_UNTIL, target.getServerWorld().getTime() + durationTicks);
        nbt.putString(KEY_LOCK_ITEM, id.toString());
    }

    public static boolean isLocked(ServerPlayerEntity player, ItemStack item) {
        if (item.isEmpty()) {
            return false;
        }
        NbtCompound nbt = persistent(player);
        long until = nbt.getLong(KEY_LOCK_UNTIL);
        if (until <= player.getServerWorld().getTime()) {
            return false;
        }
        if (!nbt.contains(KEY_LOCK_ITEM, NbtElement.STRING_TYPE)) {
            return false;
        }
        Identifier id = Registries.ITEM.getId(item.getItem());
        return id != null && id.toString().equals(nbt.getString(KEY_LOCK_ITEM));
    }

    private static NbtCompound persistent(ServerPlayerEntity player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }
}

