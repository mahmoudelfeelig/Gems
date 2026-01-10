package com.feel.gems.state;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Utility class for managing temporary and persistent player state.
 * 
 * Persistent state survives logout/death (stored in player NBT).
 * Temporary state is in-memory and cleared on logout (tick-based countdowns).
 */
public final class PlayerStateManager {
    private static final String GEMS_STATE_KEY = "GemsCustomState";
    
    // In-memory temporary state: player UUID -> key -> remaining ticks
    private static final Map<UUID, Map<String, Integer>> TEMPORARY_STATE = new ConcurrentHashMap<>();

    private PlayerStateManager() {
    }

    // ======================= PERSISTENT STATE =======================
    // Stored in player NBT, survives logout/death

    public static void setPersistent(ServerPlayerEntity player, String key, String value) {
        NbtCompound root = getOrCreateRoot(player);
        root.putString(key, value);
        persistRoot(player, root);
    }

    public static String getPersistent(ServerPlayerEntity player, String key) {
        NbtCompound root = getOrCreateRoot(player);
        if (!root.contains(key)) {
            return null;
        }
        return root.getString(key, "");
    }

    public static void clearPersistent(ServerPlayerEntity player, String key) {
        NbtCompound root = getOrCreateRoot(player);
        root.remove(key);
        persistRoot(player, root);
    }

    public static boolean hasPersistent(ServerPlayerEntity player, String key) {
        return getPersistent(player, key) != null;
    }

    private static NbtCompound getOrCreateRoot(ServerPlayerEntity player) {
        NbtCompound playerData = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        NbtCompound root = playerData.getCompound(GEMS_STATE_KEY).orElse(null);
        if (root == null) {
            root = new NbtCompound();
            playerData.put(GEMS_STATE_KEY, root);
        }
        return root;
    }

    private static void persistRoot(ServerPlayerEntity player, NbtCompound root) {
        ((GemsPersistentDataHolder) player).gems$getPersistentData().put(GEMS_STATE_KEY, root);
    }

    // ======================= TEMPORARY STATE =======================
    // In-memory only, cleared on logout, tick-based countdowns

    public static void setTemporary(ServerPlayerEntity player, String key, int ticks) {
        TEMPORARY_STATE.computeIfAbsent(player.getUuid(), k -> new ConcurrentHashMap<>()).put(key, ticks);
    }

    public static int getTemporary(ServerPlayerEntity player, String key) {
        Map<String, Integer> playerState = TEMPORARY_STATE.get(player.getUuid());
        if (playerState == null) {
            return 0;
        }
        return playerState.getOrDefault(key, 0);
    }

    public static void clearTemporary(ServerPlayerEntity player, String key) {
        Map<String, Integer> playerState = TEMPORARY_STATE.get(player.getUuid());
        if (playerState != null) {
            playerState.remove(key);
        }
    }

    public static boolean hasTemporary(ServerPlayerEntity player, String key) {
        return getTemporary(player, key) > 0;
    }

    /**
     * Called each server tick to decrement temporary state counters.
     */
    public static void tickAll() {
        for (Map.Entry<UUID, Map<String, Integer>> entry : TEMPORARY_STATE.entrySet()) {
            Map<String, Integer> playerState = entry.getValue();
            playerState.entrySet().removeIf(e -> {
                int remaining = e.getValue() - 1;
                if (remaining <= 0) {
                    return true;
                }
                e.setValue(remaining);
                return false;
            });
        }
    }

    /**
     * Called when a player logs out to clean up their temporary state.
     */
    public static void onPlayerLogout(ServerPlayerEntity player) {
        TEMPORARY_STATE.remove(player.getUuid());
    }

    /**
     * Called when a player dies to optionally clear temporary state.
     */
    public static void onPlayerDeath(ServerPlayerEntity player) {
        // Clear temporary state on death
        TEMPORARY_STATE.remove(player.getUuid());
    }
}
