package com.blissmc.gems.trust;

import com.blissmc.gems.state.GemsPersistentDataHolder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GemTrust {
    private static final String KEY_TRUSTED = "trustedPlayers";
    private static final String KEY_TRUST_VERSION = "trustedPlayersVersion";

    private static final Map<UUID, CacheEntry> CACHE = new ConcurrentHashMap<>();

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
        int version = root.getInt(KEY_TRUST_VERSION);
        UUID ownerId = owner.getUuid();
        CacheEntry cached = CACHE.get(ownerId);
        if (cached != null && cached.version == version) {
            return cached.trusted;
        }

        if (!root.contains(KEY_TRUSTED, NbtElement.LIST_TYPE)) {
            Set<UUID> empty = Set.of();
            CACHE.put(ownerId, new CacheEntry(version, empty));
            return empty;
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
        Set<UUID> frozen = Set.copyOf(result);
        CACHE.put(ownerId, new CacheEntry(version, frozen));
        return frozen;
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
        NbtCompound root = root(owner);
        root.put(KEY_TRUSTED, list);
        int nextVersion = root.getInt(KEY_TRUST_VERSION) + 1;
        root.putInt(KEY_TRUST_VERSION, nextVersion);
        CACHE.put(owner.getUuid(), new CacheEntry(nextVersion, Set.copyOf(uuids)));
    }

    private static NbtCompound root(PlayerEntity player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }

    public static void clearRuntimeCache(UUID ownerUuid) {
        CACHE.remove(ownerUuid);
    }

    private static final class CacheEntry {
        final int version;
        final Set<UUID> trusted;

        private CacheEntry(int version, Set<UUID> trusted) {
            this.version = version;
            this.trusted = trusted;
        }
    }
}
