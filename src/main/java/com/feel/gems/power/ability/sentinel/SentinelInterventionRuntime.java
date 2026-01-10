package com.feel.gems.power.ability.sentinel;

import com.feel.gems.state.PlayerStateManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.UUID;

public final class SentinelInterventionRuntime {
    private static final String PROTECTING_KEY = "sentinel_intervention_protecting";
    private static final String PROTECTED_BY_KEY = "sentinel_intervention_protected_by";

    private SentinelInterventionRuntime() {}

    public static void setProtecting(ServerPlayerEntity sentinel, ServerPlayerEntity ally) {
        if (sentinel == null || ally == null) {
            return;
        }
        if (sentinel == ally) {
            return;
        }
        MinecraftServer server = sentinel.getEntityWorld().getServer();
        if (server == null) {
            return;
        }

        // Clear any previous protection the sentinel had.
        UUID prevAlly = getProtectedAlly(sentinel);
        if (prevAlly != null) {
            ServerPlayerEntity prev = server.getPlayerManager().getPlayer(prevAlly);
            if (prev != null) {
                clearProtectedBy(prev, sentinel.getUuid());
            }
        }

        // Clear any existing protector on the ally.
        UUID prevProtector = getProtectorUuid(ally);
        if (prevProtector != null && !prevProtector.equals(sentinel.getUuid())) {
            ServerPlayerEntity prevSentinel = server.getPlayerManager().getPlayer(prevProtector);
            if (prevSentinel != null) {
                clearProtecting(prevSentinel, ally.getUuid());
            }
        }

        PlayerStateManager.setPersistent(sentinel, PROTECTING_KEY, ally.getUuidAsString());
        PlayerStateManager.setPersistent(ally, PROTECTED_BY_KEY, sentinel.getUuidAsString());
    }

    public static UUID getProtectedAlly(ServerPlayerEntity sentinel) {
        String allyStr = PlayerStateManager.getPersistent(sentinel, PROTECTING_KEY);
        if (allyStr == null || allyStr.isEmpty()) return null;
        try {
            return UUID.fromString(allyStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean isProtecting(ServerPlayerEntity sentinel) {
        return getProtectedAlly(sentinel) != null;
    }

    public static void consumeProtection(ServerPlayerEntity sentinel) {
        if (sentinel == null) {
            return;
        }
        MinecraftServer server = sentinel.getEntityWorld().getServer();
        UUID ally = getProtectedAlly(sentinel);
        PlayerStateManager.clearPersistent(sentinel, PROTECTING_KEY);
        if (server != null && ally != null) {
            ServerPlayerEntity allyPlayer = server.getPlayerManager().getPlayer(ally);
            if (allyPlayer != null) {
                clearProtectedBy(allyPlayer, sentinel.getUuid());
            }
        }
    }

    /**
     * Called when the protected ally would take damage.
     * Returns the sentinel who should take the damage instead, or null if no protection.
     */
    public static ServerPlayerEntity getProtector(ServerPlayerEntity ally) {
        if (ally == null) {
            return null;
        }
        MinecraftServer server = ally.getEntityWorld().getServer();
        if (server == null) {
            return null;
        }
        UUID protectorId = getProtectorUuid(ally);
        if (protectorId == null) {
            return null;
        }
        ServerPlayerEntity sentinel = server.getPlayerManager().getPlayer(protectorId);
        if (sentinel == null) {
            PlayerStateManager.clearPersistent(ally, PROTECTED_BY_KEY);
            return null;
        }
        UUID protectedId = getProtectedAlly(sentinel);
        if (protectedId == null || !protectedId.equals(ally.getUuid())) {
            PlayerStateManager.clearPersistent(ally, PROTECTED_BY_KEY);
            return null;
        }
        return sentinel;
    }

    public static void clearProtection(ServerPlayerEntity sentinel) {
        consumeProtection(sentinel);
    }

    public static void cleanup(MinecraftServer server, ServerPlayerEntity player) {
        if (server == null || player == null) {
            return;
        }

        UUID ally = getProtectedAlly(player);
        if (ally != null) {
            ServerPlayerEntity allyPlayer = server.getPlayerManager().getPlayer(ally);
            PlayerStateManager.clearPersistent(player, PROTECTING_KEY);
            if (allyPlayer != null) {
                clearProtectedBy(allyPlayer, player.getUuid());
            }
        }

        UUID protector = getProtectorUuid(player);
        if (protector != null) {
            ServerPlayerEntity sentinel = server.getPlayerManager().getPlayer(protector);
            PlayerStateManager.clearPersistent(player, PROTECTED_BY_KEY);
            if (sentinel != null) {
                clearProtecting(sentinel, player.getUuid());
            }
        }
    }

    private static UUID getProtectorUuid(ServerPlayerEntity ally) {
        String uuidStr = PlayerStateManager.getPersistent(ally, PROTECTED_BY_KEY);
        if (uuidStr == null || uuidStr.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static void clearProtecting(ServerPlayerEntity sentinel, UUID allyId) {
        UUID current = getProtectedAlly(sentinel);
        if (current != null && current.equals(allyId)) {
            PlayerStateManager.clearPersistent(sentinel, PROTECTING_KEY);
        }
    }

    private static void clearProtectedBy(ServerPlayerEntity ally, UUID sentinelId) {
        UUID current = getProtectorUuid(ally);
        if (current != null && current.equals(sentinelId)) {
            PlayerStateManager.clearPersistent(ally, PROTECTED_BY_KEY);
        }
    }
}
