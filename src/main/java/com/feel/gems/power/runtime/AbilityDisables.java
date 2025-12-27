package com.feel.gems.power.runtime;

import com.feel.gems.state.GemsPersistentDataHolder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
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
        if (!root.contains(KEY_DISABLED, NbtElement.LIST_TYPE)) {
            return false;
        }
        NbtList list = root.getList(KEY_DISABLED, NbtElement.STRING_TYPE);
        String needle = abilityId.toString();
        for (int i = 0; i < list.size(); i++) {
            if (needle.equals(list.getString(i))) {
                return true;
            }
        }
        return false;
    }

    public static boolean disable(ServerPlayerEntity player, Identifier abilityId) {
        NbtCompound root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        NbtList list = root.contains(KEY_DISABLED, NbtElement.LIST_TYPE)
                ? root.getList(KEY_DISABLED, NbtElement.STRING_TYPE)
                : new NbtList();
        String raw = abilityId.toString();
        for (int i = 0; i < list.size(); i++) {
            if (raw.equals(list.getString(i))) {
                root.put(KEY_DISABLED, list);
                return false;
            }
        }
        list.add(NbtString.of(raw));
        root.put(KEY_DISABLED, list);
        return true;
    }

    public static void clear(ServerPlayerEntity player) {
        ((GemsPersistentDataHolder) player).gems$getPersistentData().remove(KEY_DISABLED);
    }
}

