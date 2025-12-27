package com.feel.gems.state;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;




/**
 * Small helpers for persistent per-player NBT.
 */
public final class PlayerNbt {
    private PlayerNbt() {
    }

    public static int getInt(ServerPlayerEntity player, String key, int fallback) {
        NbtCompound nbt = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        if (!nbt.contains(key, NbtElement.INT_TYPE)) {
            return fallback;
        }
        return nbt.getInt(key);
    }

    public static void putInt(ServerPlayerEntity player, String key, int value) {
        ((GemsPersistentDataHolder) player).gems$getPersistentData().putInt(key, value);
    }
}

