package com.feel.gems.power.runtime;

import com.feel.gems.state.GemsPersistentDataHolder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;




/**
 * Per-player ability disables (used by Spy/Mimic theft).
 *
 * <p>Stored as persistent player NBT and checked on every ability activation.</p>
 */
public final class AbilityDisables {
    private static final String KEY_DISABLED = "gemsDisabledAbilities";

    private AbilityDisables() {
    }

    public static boolean isDisabled(ServerPlayerEntity player, Identifier abilityId) {
        NbtCompound root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        String needle = abilityId.toString();
        NbtCompound set = root.getCompound(KEY_DISABLED).orElse(null);
        if (set != null) {
            return set.contains(needle);
        }
        NbtList legacy = root.getList(KEY_DISABLED).orElse(null);
        if (legacy == null || legacy.isEmpty()) {
            return false;
        }
        for (int i = 0; i < legacy.size(); i++) {
            if (needle.equals(legacy.getString(i, ""))) {
                return true;
            }
        }
        return false;
    }

    public static boolean disable(ServerPlayerEntity player, Identifier abilityId) {
        NbtCompound root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        String raw = abilityId.toString();
        NbtCompound set = getOrCreateSet(root);
        if (set.contains(raw)) {
            return false;
        }
        set.putBoolean(raw, true);
        root.put(KEY_DISABLED, set);
        return true;
    }

    public static void clear(ServerPlayerEntity player) {
        ((GemsPersistentDataHolder) player).gems$getPersistentData().remove(KEY_DISABLED);
    }

    public static boolean enable(ServerPlayerEntity player, Identifier abilityId) {
        NbtCompound root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        String needle = abilityId.toString();
        boolean removed = false;

        NbtCompound set = root.getCompound(KEY_DISABLED).orElse(null);
        if (set != null) {
            removed = set.remove(needle) != null;
            if (set.isEmpty()) {
                root.remove(KEY_DISABLED);
            } else {
                root.put(KEY_DISABLED, set);
            }
            return removed;
        }

        NbtList legacy = root.getList(KEY_DISABLED).orElse(null);
        if (legacy == null || legacy.isEmpty()) {
            return false;
        }
        for (int i = legacy.size() - 1; i >= 0; i--) {
            if (needle.equals(legacy.getString(i, ""))) {
                legacy.remove(i);
                removed = true;
            }
        }
        if (legacy.isEmpty()) {
            root.remove(KEY_DISABLED);
        } else {
            root.put(KEY_DISABLED, legacy);
        }
        return removed;
    }

    private static NbtCompound getOrCreateSet(NbtCompound root) {
        NbtCompound existing = root.getCompound(KEY_DISABLED).orElse(null);
        if (existing != null) {
            return existing;
        }
        NbtCompound migrated = new NbtCompound();
        NbtList legacy = root.getList(KEY_DISABLED).orElse(null);
        if (legacy != null && !legacy.isEmpty()) {
            for (int i = 0; i < legacy.size(); i++) {
                String id = legacy.getString(i, "");
                if (!id.isBlank()) {
                    migrated.putBoolean(id, true);
                }
            }
        }
        return migrated;
    }
}

