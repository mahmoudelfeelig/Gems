package com.feel.gems.state;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;




/**
 * Small helpers for persistent per-player NBT.
 */
public final class PlayerNbt {
    private PlayerNbt() {
    }

    public static boolean getBoolean(ServerPlayerEntity player, String key, boolean fallback) {
        NbtCompound nbt = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        return nbt.getBoolean(key).orElse(fallback);
    }

    public static void putBoolean(ServerPlayerEntity player, String key, boolean value) {
        ((GemsPersistentDataHolder) player).gems$getPersistentData().putBoolean(key, value);
    }

    public static int getInt(ServerPlayerEntity player, String key, int fallback) {
        NbtCompound nbt = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        return nbt.getInt(key).orElse(fallback);
    }

    public static void putInt(ServerPlayerEntity player, String key, int value) {
        ((GemsPersistentDataHolder) player).gems$getPersistentData().putInt(key, value);
    }
}

