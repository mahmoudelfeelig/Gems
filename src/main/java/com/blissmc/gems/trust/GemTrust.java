package com.blissmc.gems.trust;

import com.blissmc.gems.state.GemsPersistentDataHolder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class GemTrust {
    private static final String KEY_TRUSTED = "trustedPlayers";

    private GemTrust() {
    }

    public static boolean isTrusted(ServerPlayerEntity owner, ServerPlayerEntity other) {
        if (owner == other) {
            return true;
        }
        return getTrusted(owner).contains(other.getUuid());
    }

    public static Set<UUID> getTrusted(PlayerEntity owner) {
        NbtCompound root = root(owner);
        if (!root.contains(KEY_TRUSTED, NbtElement.LIST_TYPE)) {
            return Set.of();
        }
        NbtList list = root.getList(KEY_TRUSTED, NbtElement.INT_ARRAY_TYPE);
        Set<UUID> result = new HashSet<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            try {
                result.add(NbtHelper.toUuid(list.get(i)));
            } catch (IllegalArgumentException ignored) {
                // ignore malformed entries
            }
        }
        return result;
    }

    public static boolean trust(PlayerEntity owner, UUID uuid) {
        Set<UUID> current = new HashSet<>(getTrusted(owner));
        boolean changed = current.add(uuid);
        if (changed) {
            writeTrusted(owner, current);
        }
        return changed;
    }

    public static boolean untrust(PlayerEntity owner, UUID uuid) {
        Set<UUID> current = new HashSet<>(getTrusted(owner));
        boolean changed = current.remove(uuid);
        if (changed) {
            writeTrusted(owner, current);
        }
        return changed;
    }

    private static void writeTrusted(PlayerEntity owner, Set<UUID> uuids) {
        NbtList list = new NbtList();
        for (UUID uuid : uuids) {
            list.add(NbtHelper.fromUuid(uuid));
        }
        root(owner).put(KEY_TRUSTED, list);
    }

    private static NbtCompound root(PlayerEntity player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }
}

