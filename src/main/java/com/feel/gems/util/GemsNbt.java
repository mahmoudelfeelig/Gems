package com.feel.gems.util;

import java.util.UUID;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Uuids;

public final class GemsNbt {
    private GemsNbt() {
    }

    public static void putUuid(NbtCompound nbt, String key, UUID uuid) {
        if (uuid == null) {
            nbt.remove(key);
            return;
        }
        nbt.putIntArray(key, Uuids.toIntArray(uuid));
    }

    public static UUID getUuid(NbtCompound nbt, String key) {
        if (nbt == null) {
            return null;
        }
        return nbt.getIntArray(key)
                .filter(arr -> arr.length == 4)
                .map(Uuids::toUuid)
                .orElseGet(() -> nbt.getString(key).map(raw -> {
                    try {
                        return UUID.fromString(raw);
                    } catch (IllegalArgumentException ignored) {
                        return null;
                    }
                }).orElse(null));
    }

    public static boolean containsUuid(NbtCompound nbt, String key) {
        if (nbt == null) {
            return false;
        }
        return nbt.getIntArray(key).isPresent() || nbt.getString(key).isPresent();
    }

    public static NbtElement fromUuid(UUID uuid) {
        if (uuid == null) {
            return NbtString.of("");
        }
        return new NbtIntArray(Uuids.toIntArray(uuid));
    }

    public static UUID toUuid(NbtElement element) {
        if (element instanceof NbtIntArray array) {
            int[] raw = array.getIntArray();
            if (raw.length == 4) {
                return Uuids.toUuid(raw);
            }
            return null;
        }
        if (element instanceof NbtString str) {
            String raw = str.asString().orElse("");
            if (raw.isBlank()) {
                return null;
            }
            try {
                return UUID.fromString(raw);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }
}
